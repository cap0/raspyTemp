package gg;

import gg.TemperatureSetting.TemperatureSettings;
import gg.TemperatureSetting.TemperatureSettingsFileHandler;
import gg.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Properties;

import static gg.util.PropertyUtil.*;
import static gg.util.Util.formatTemperature;
import static gg.util.Util.toDouble;

public class Controller implements Runnable{

    private static final Logger logger = LogManager.getLogger(Controller.class);
    private static volatile Double actualDeltaTemp;
    private final Double deltaTempWhenFermenting;
    private Double deltaTempWhenCoolingOrWarming;

    private final String temperatureSettingsPath;
    private final IGPIOController gpioCtrl;
    private final LCD lcd;
    private ConnectionChecker connCheck;

    private IReadTemperature temperatureReader;

    Controller(Properties p, ConnectionChecker connCheck, LCD lcd, GPIOController gpioCtrl) {
        this(new TemperatureReader(p.getProperty(SENSORS_FOLDER),
                        p.getProperty(WORT_SENSOR),
                        p.getProperty(ROOM_SENSOR)),
                getTemperatureSettingsPath(p),
                getDeltaTempFromProperties(p),
                getDeltaTempActiveFromProperties(p),
                gpioCtrl, lcd, connCheck);
    }

    private Controller(IReadTemperature temperatureReader, String temperatureSettingsPath,
                       Double configDeltaTemp, Double deltaTempWhenCoolingOrWarming, IGPIOController gpioCtrl, LCD lcd, ConnectionChecker connCheck) {
        this.temperatureReader = temperatureReader;
        this.temperatureSettingsPath = temperatureSettingsPath;
        this.deltaTempWhenFermenting = configDeltaTemp;
        this.deltaTempWhenCoolingOrWarming = deltaTempWhenCoolingOrWarming;
        this.gpioCtrl = gpioCtrl;
        this.lcd = lcd;
        this.connCheck = connCheck;
        this.actualDeltaTemp = deltaTempWhenFermenting;
    }


    private void checkIfTempIsOnRange(LocalDateTime now) {
        logger.debug("checking temperature " + now);

        Double wortTemp = toDouble(temperatureReader.getWorthTemperature());

        TemperatureSettings temperatureSettings = new TemperatureSettings(new TemperatureSettingsFileHandler(temperatureSettingsPath));
        temperatureSettings.initialize();
        double settingTemp = temperatureSettings.getTemperatureSettingsValueForDate(now);

        double lowerBound = getLowerBound(settingTemp);
        double upperBound = getUpperBound(settingTemp);

        String roomTemp = temperatureReader.getRoomTemperature();
        if (wortTemp > upperBound) {
            cooling();
            actualDeltaTemp = deltaTempWhenCoolingOrWarming;
        } else if (wortTemp < lowerBound) {
            heating();
            actualDeltaTemp = deltaTempWhenCoolingOrWarming;
        } else { // temp in range
            stopHeatingOrCooling();
            actualDeltaTemp = deltaTempWhenFermenting;
        }

        String connStatus = connCheck.isConnectionAvailable() ? "V" : "X";
        lcd(gpioCtrl.getStatus(), wortTemp, roomTemp, getLowerBound(settingTemp), getUpperBound(settingTemp), connStatus);
    }

    private void lcd(Status status, Double wortTemp, String roomTemp, double lowerBound, double upperBound, String connStatus) {
        String row0 = "R " + roomTemp + " W " + wortTemp;
        String row1 = status + " "+ formatTempRangeForLcd(lowerBound, upperBound) + " " +connStatus;
        lcd.print(row0, row1);
        logger.info(row0 + " " + row1);
    }

    private String formatTempRangeForLcd(double lowerBound, double upperBound) {
        return formatTemperature(lowerBound) + "-" + formatTemperature(upperBound) ;
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
        } catch (Throwable e) {
            logger.error(e);
        }
    }

    private static Double getDeltaTempFromProperties(Properties p) {
        return Double.valueOf(p.getProperty(CTRL_DELTA_TEMP, "0.5")); //TODO read changed file
    }

    private static Double getDeltaTempActiveFromProperties(Properties p) {
        return Double.valueOf(p.getProperty(CTRL_DELTA_TEMP_ACTIVE, "0.1")); //TODO read changed file
    }

    private double getUpperBound(double settingTemp) {
        return settingTemp + actualDeltaTemp;
    }

    private double getLowerBound(double settingTemp) {
        return settingTemp - actualDeltaTemp;
    }
}
