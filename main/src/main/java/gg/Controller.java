package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Properties;

import static gg.Constants.*;
import static gg.Util.toDouble;

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
               null, //TODO
                //readTemperatureSettings(p),
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


    public void checkIfTempIsOnRange(LocalDateTime now) {
        double wortTemp = getWortTemp();//todo repeat 3 times to check the stability

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

    private Double getWortTemp() {
        String wortTemperatureValue = temperatureReader.readTemperatureForSensor(wortSensorName);
        return toDouble(wortTemperatureValue);
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
