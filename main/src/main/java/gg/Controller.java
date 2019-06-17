package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static gg.Constants.*;
import static gg.Util.formatTemperature;
import static gg.Util.toDouble;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
    private TemperatureSettings temperatureSettings;

    Controller(Properties p, ConnectionChecker connCheck, LCD lcd, GPIOController gpioCtrl) {
        this(new TemperatureReader(p.getProperty(SENSORS_FOLDER),
                        p.getProperty(WORT_SENSOR),
                        p.getProperty(ROOM_SENSOR)),
                p.getProperty(TEMPERATURE_SETTINGS_FILE_PATH),
                getDeltaTempFromProperties(p),
                getDeltaTempActiveFromProperties(p),
                gpioCtrl, lcd, connCheck);
    }

    private Controller(IReadTemperature temperatureReader, String temperatureSettingsPath,
                       Double configDeltaTemp, Double deltaTempWhenCoolingOrWarming, IGPIOController gpioCtrl, LCD lcd, ConnectionChecker connCheck) {
        this.temperatureReader = temperatureReader;
        this.temperatureSettingsPath = temperatureSettingsPath;
        this.temperatureSettings = new TemperatureSettings(temperatureSettingsPath);
        this.deltaTempWhenFermenting = configDeltaTemp;
        this.deltaTempWhenCoolingOrWarming = deltaTempWhenCoolingOrWarming;
        this.gpioCtrl = gpioCtrl;
        this.lcd = lcd;
        this.connCheck = connCheck;
        this.actualDeltaTemp = deltaTempWhenFermenting;
    }


    private void checkIfTempIsOnRange(LocalDateTime now) {
        logger.debug("checking temperature " + now);
       /* Optional<Double> wortTempOpt = getWortTemp();
        if (!wortTempOpt.isPresent()) {
            return;
        }
        Double wortTemp = wortTempOpt.get();
        */

        Double wortTemp = toDouble(temperatureReader.getWorthTemperature());

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
