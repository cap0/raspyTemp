package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static gg.Constants.*;
import static gg.Util.toDouble;

public class Controller implements Runnable{

    private static final Logger logger = LogManager.getLogger(Controller.class);
    private final Double deltaTemp;
    private final GPIOController gpioCtrl;

    private ReadTemperature temperatureReader;
    private String wortSensorName;

    private Map<LocalDateTime, Double> temperatureSettings = new HashMap<>();

    public Controller(Properties p) {
        temperatureReader = new ReadTemperature(p.getProperty(SENSORS_FOLDER));
        wortSensorName = p.getProperty(WORT_SENSOR);

        deltaTemp = Double.valueOf(p.getProperty(CTRL_DELTA_TEMP,"0.5"));

        gpioCtrl = new GPIOController();
    }

    public void checkIfTempIsOnRange() {
        double wortTemp = getWortTemp();//todo repeat 3 times

        double settingTemp = getSettingTempForThisMoment();

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

    private double getSettingTempForThisMoment() {
        return 0;//todo impl
    }

    private void heating() {
        //trigger relay heating belt
        gpioCtrl.startBelt();
    }

    private void cooling() {
        gpioCtrl.startFridge();
        //trigger relay fridge
        //pump protection
    }

    @Override
    public void run() {
        try {
            checkIfTempIsOnRange();
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
