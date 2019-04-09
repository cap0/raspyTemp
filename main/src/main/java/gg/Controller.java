package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static gg.Constants.*;
import static gg.Controller.Status.*;
import static gg.Util.formatTemperature;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Controller implements Runnable{

    private static final Logger logger = LogManager.getLogger(Controller.class);
    private static final double DELTA_TEMP_WHEN_ACTIVE = 0.1;

    private final Double configDeltaTemp;
    private Double actualDeltaTemp;
    private final IGPIOController gpioCtrl;
    private final LCD lcd;

    private IReadTemperature temperatureReader;
    private TemperatureSettings temperatureSettings;

    Controller(Properties p, LCD lcd) {
        this(new TemperatureReader(p.getProperty(SENSORS_FOLDER),
                p.getProperty(WORT_SENSOR),
                p.getProperty(ROOM_SENSOR)),
               new TemperatureSettings(p),
                getDeltaTempFromProperties(p),
                getDeltaTempFromProperties(p),
                new GPIOController(), lcd);
    }

    private Controller(IReadTemperature temperatureReader, TemperatureSettings temperatureSettings,
                       Double configDeltaTemp, Double actualDeltaTemp,IGPIOController gpioCtrl, LCD lcd) {
        this.temperatureReader = temperatureReader;
        this.temperatureSettings = temperatureSettings;
        this.configDeltaTemp = configDeltaTemp;
        this.actualDeltaTemp = actualDeltaTemp;
        this.gpioCtrl = gpioCtrl;
        this.lcd = lcd;
    }


    private void checkIfTempIsOnRange(LocalDateTime now) {
        Optional<Double> wortTempOpt = getWortTemp();
        if (!wortTempOpt.isPresent()) {
            return;
        }
        Double wortTemp = wortTempOpt.get();

        double settingTemp = temperatureSettings.getTemperatureSettingsValueForDate(now);

        double upperBound = settingTemp + actualDeltaTemp;
        double lowerBound = settingTemp - actualDeltaTemp;

        String roomTemp = temperatureReader.getRoomTemperature();
        if (wortTemp > upperBound) {
            lcd(cold, wortTemp, roomTemp, lowerBound, upperBound);
            cooling();
            actualDeltaTemp = DELTA_TEMP_WHEN_ACTIVE;
        } else if (wortTemp < lowerBound) {
            lcd(warm, wortTemp, roomTemp, lowerBound, upperBound);
            heating();
            actualDeltaTemp = DELTA_TEMP_WHEN_ACTIVE;
        } else { // temp in range
            lcd(ferm, wortTemp, roomTemp, lowerBound, upperBound);
            stopHeatingOrCooling();
            actualDeltaTemp = configDeltaTemp;
        }
    }

    private void lcd(Status cool, Double wortTemp, String roomTemp, double lowerBound, double upperBound) {
        String row0 = "R " + roomTemp + " W " + wortTemp;
        String row1 = cool + " "+ formatTempRangeForLcd(lowerBound, upperBound);
        lcd.print(row0, row1);
        logger.info(row0 + " " + row1);
    }

    private String formatTempRangeForLcd(double lowerBound, double upperBound) {
        return formatTemperature(lowerBound) + "-" + formatTemperature(upperBound) ;
    }

    private Optional<Double> getWortTemp() {
        String wortTemperatureValue1 = temperatureReader.getWorthTemperature();
        sleep1Sec();
        String wortTemperatureValue2 = temperatureReader.getWorthTemperature();
        sleep1Sec();
        String wortTemperatureValue3 = temperatureReader.getWorthTemperature();

        if (isNotBlank(wortTemperatureValue1) && isNotBlank(wortTemperatureValue2) && isNotBlank(wortTemperatureValue3)) {
            Double t1 = Double.valueOf(wortTemperatureValue1);
            Double t2 = Double.valueOf(wortTemperatureValue2);
            Double t3 = Double.valueOf(wortTemperatureValue3);
            double delta = t1 - t2 + t2 - t3 + t1 - t3;
            boolean tempIsStable = delta <= 0.1;
            if (tempIsStable) {
                return Optional.of(t1);
            }
        }
        logger.warn("temperature not stable: {}, {}, {}", wortTemperatureValue1, wortTemperatureValue2, wortTemperatureValue3);
        return Optional.empty();
    }

    private void sleep1Sec() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    private void heating() {
        gpioCtrl.startBelt();
    }

    private void cooling() {
        gpioCtrl.startFridge();
        //TODO pump protection
    }

    private void stopHeatingOrCooling() {
        gpioCtrl.stop();
    }

    @Override
    public void run() {
        try {
            checkIfTempIsOnRange(LocalDateTime.now());
        } catch (Exception e) {
            logger.error(e);
        } finally {
            schedule();
        }
    }

    private void schedule() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new Controller(temperatureReader, temperatureSettings, configDeltaTemp, actualDeltaTemp, gpioCtrl,lcd);
        scheduler.schedule(task, 1, MINUTES);
        logger.info("controller in a minute: " + configDeltaTemp);
    }

    private static Double getDeltaTempFromProperties(Properties p) {
        return Double.valueOf(p.getProperty(CTRL_DELTA_TEMP, "0.5"));
    }

    enum Status{
        warm, cold, ferm;
    }
}
