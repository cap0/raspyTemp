package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static gg.Constants.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Controller implements Runnable{

    private static final Logger logger = LogManager.getLogger(Controller.class);
    private final Double deltaTemp;
    final IGPIOController gpioCtrl;

    IReadTemperature temperatureReader;
    private String wortSensorName;

    private TemperatureSettings temperatureSettings;

    public Controller(Properties p) {
        this(new TemperatureReader(p.getProperty(SENSORS_FOLDER)),
                p.getProperty(WORT_SENSOR),
               new TemperatureSettings(p),
                Double.valueOf(p.getProperty(CTRL_DELTA_TEMP,"0.5")),
                new GPIOController());
    }

    public Controller(IReadTemperature temperatureReader, String wortSensorName, TemperatureSettings temperatureSettings,
                      Double deltaTemp, IGPIOController gpioCtrl) {
        this.temperatureReader = temperatureReader;
        this.wortSensorName = wortSensorName;
        this.temperatureSettings = temperatureSettings;
        this.deltaTemp = deltaTemp;
        this.gpioCtrl = gpioCtrl;
    }


    void checkIfTempIsOnRange(LocalDateTime now) {
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
            cooling();
        } else if (wortTemp < lowerBound) {
            logger.info("wort temperature under setting value. Wort: " + wortTemp + " lower bound: " + upperBound);
            heating();
        } else { // temp in range
            stopHeatingOrCooling();
            logger.info("temperature " + wortTemp + " inside range (" + lowerBound + "," + upperBound + ")");
        }
    }

    private void stopHeatingOrCooling() {
        gpioCtrl.stop();
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
            if (delta <=0.1) {
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
        //trigger relay heating belt
        gpioCtrl.startBelt();
    }

    private void cooling() {
        gpioCtrl.startFridge();
        //trigger relay fridge
        //TODO pump protection
    }

    @Override
    public void run() {
        try {
            checkIfTempIsOnRange(LocalDateTime.now());
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
