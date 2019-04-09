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
import static gg.Util.formatTemperature;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Controller implements Runnable{

    private static final Logger logger = LogManager.getLogger(Controller.class);
    private static final double DELTA_TEMP_WHEN_ACTIVE = 0.1;

    private Double deltaTemp;
    private final IGPIOController gpioCtrl;
    private final Properties p;
    private final LCD lcd;

    private IReadTemperature temperatureReader;
    private String wortSensorName;

    private TemperatureSettings temperatureSettings;

    Controller(Properties p, LCD lcd) {
        this(new TemperatureReader(p.getProperty(SENSORS_FOLDER)),
                p.getProperty(WORT_SENSOR),
               new TemperatureSettings(p),
                getDeltaTempFromProperties(p),
                new GPIOController(), lcd, p);
    }

    private Controller(Properties p, double deltaTemp, IGPIOController gpioCtrl, TemperatureReader temperatureReader, LCD lcd) {
        this(temperatureReader,
                p.getProperty(WORT_SENSOR),
                new TemperatureSettings(p),
                deltaTemp,
                gpioCtrl, lcd, p);
    }

    private Controller(IReadTemperature temperatureReader, String wortSensorName, TemperatureSettings temperatureSettings,
                       Double deltaTemp, IGPIOController gpioCtrl, LCD lcd, Properties p) {
        this.temperatureReader = temperatureReader;
        this.wortSensorName = wortSensorName;
        this.temperatureSettings = temperatureSettings;
        this.deltaTemp = deltaTemp;
        this.gpioCtrl = gpioCtrl;
        this.p = p;
        this.lcd = lcd;
    }


    private void checkIfTempIsOnRange(LocalDateTime now) {
        Optional<Double> wortTempOpt = getWortTemp();
        if (!wortTempOpt.isPresent()) {
            return;
        }
        Double wortTemp = wortTempOpt.get();

        double settingTemp = temperatureSettings.getTemperatureSettingsValueForDate(now);

        double upperBound = settingTemp + deltaTemp;
        double lowerBound = settingTemp - deltaTemp;

        if (wortTemp > upperBound) {
            logger.info("wort temperature over setting value. Wort: " + wortTemp + " upper bound: " + upperBound);
            lcd("cool: ", lowerBound, upperBound);
            cooling();
            deltaTemp = DELTA_TEMP_WHEN_ACTIVE;
        } else if (wortTemp < lowerBound) {
            logger.info("wort: temperature under setting value. Wort: " + wortTemp + " lower bound: " + upperBound);
            lcd("heat ", lowerBound, upperBound);
            heating();
            deltaTemp = DELTA_TEMP_WHEN_ACTIVE;
        } else { // temp in range
            logger.info("temperature " + wortTemp + " inside range (" + lowerBound + "," + upperBound + ")");
            lcd("ferm: ", lowerBound, upperBound);
            stopHeatingOrCooling();
            deltaTemp = getDeltaTempFromProperties(p);
        }
    }

    private void lcd(String status, double lowerBound, double upperBound) {
        lcd.print0(status + formatTempRangeForLcd(lowerBound, upperBound));
    }

    private String formatTempRangeForLcd(double lowerBound, double upperBound) {
        return formatTemperature(lowerBound) + "-" + formatTemperature(upperBound) ;
    }

    private Optional<Double> getWortTemp() {
        String wortTemperatureValue1 = temperatureReader.readTemperatureForSensor(wortSensorName);
        sleep1Sec();
        String wortTemperatureValue2 = temperatureReader.readTemperatureForSensor(wortSensorName);
        sleep1Sec();
        String wortTemperatureValue3 = temperatureReader.readTemperatureForSensor(wortSensorName);

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
        Runnable task = new Controller(p, deltaTemp, gpioCtrl, new TemperatureReader(p.getProperty(SENSORS_FOLDER)), lcd);
        scheduler.schedule(task, 1, MINUTES);
        logger.info("controller in a minute: " + deltaTemp);
    }

    private static Double getDeltaTempFromProperties(Properties p) {
        return Double.valueOf(p.getProperty(CTRL_DELTA_TEMP, "0.5"));
    }
}
