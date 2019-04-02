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
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Controller implements Runnable{

    private static final Logger logger = LogManager.getLogger(Controller.class);
    private static final double DELTA_TEMP_WHEN_ACTIVE = 0.1;

    private final Double deltaTemp;
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

        double nextDeltaTemp;
        if (wortTemp > upperBound) {
            logger.info("wort temperature over setting value. Wort: " + wortTemp + " upper bound: " + upperBound);
            cooling();
            nextDeltaTemp=DELTA_TEMP_WHEN_ACTIVE;
        } else if (wortTemp < lowerBound) {
            logger.info("wort temperature under setting value. Wort: " + wortTemp + " lower bound: " + upperBound);
            heating();
            nextDeltaTemp= DELTA_TEMP_WHEN_ACTIVE;
        } else { // temp in range
            stopHeatingOrCooling();
            logger.info("temperature " + wortTemp + " inside range (" + lowerBound + "," + upperBound + ")");
            nextDeltaTemp=getDeltaTempFromProperties(p);
        }

        schedule(nextDeltaTemp);
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
        lcd.print0("heating...");
        gpioCtrl.startBelt();
    }

    private void cooling() {
        lcd.print0("cooling...");
        gpioCtrl.startFridge();
        //TODO pump protection
    }

    private void stopHeatingOrCooling() {
        lcd.print0("fermenting...");
        gpioCtrl.stop();
    }

    @Override
    public void run() {
        try {
            checkIfTempIsOnRange(LocalDateTime.now());
        } catch (Exception e) {
            logger.error(e);
            schedule(getDeltaTempFromProperties(p));
        }
    }

    private void schedule(double deltaTemp) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new Controller(p, deltaTemp, gpioCtrl, new TemperatureReader(p.getProperty(SENSORS_FOLDER)), lcd);
        scheduler.schedule(task, 1, MINUTES);
        logger.info("controller in a minute: " + deltaTemp);
    }

    private static Double getDeltaTempFromProperties(Properties p) {
        return Double.valueOf(p.getProperty(CTRL_DELTA_TEMP, "0.5"));
    }
}
