package gg;

import gg.util.SensorType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static gg.util.PropertyUtil.*;
import static gg.util.Util.toDouble;

public class TemperatureAlarm implements Runnable {
    private static final Logger logger = LogManager.getLogger(TemperatureAlarm.class);

    private static final int NUMBER_OF_VIOLATIONS = 3;

    private static final double WORT_LOWER_TEMPERATURE_LIMIT = 0;
    private static final double WORT_UPPER_TEMPERATURE_LIMIT = 25;

    private static final double ROOM_LOWER_TEMPERATURE_LIMIT = -5;
    private static final double ROOM_UPPER_TEMPERATURE_LIMIT = 40;

    private Map<SensorType,Integer> lowerViolationsInArow = new HashMap<>();
    private Map<SensorType,Integer> upperViolationsInArow = new HashMap<>();

    IReadTemperature temperatureReader;
    IGMailSender mailSender;

    TemperatureAlarm(Properties p) {
        temperatureReader = new TemperatureReader(p.getProperty(SENSORS_FOLDER), p.getProperty(WORT_SENSOR),  p.getProperty(ROOM_SENSOR));
        mailSender = new GMailSender(p);

        lowerViolationsInArow.put(SensorType.wort, 0);
        lowerViolationsInArow.put(SensorType.room, 0);
    }

    @Override
    public void run() {
        logger.debug("checking temperature for alarm");
        checkWortTempForAlarm();
        checkRoomTempForAlarm();
    }

    private void checkWortTempForAlarm() {
        try {
            String wortTemperatureValue = temperatureReader.getWorthTemperature();
            checkViolationAndSendAlarm(wortTemperatureValue, SensorType.wort);
        } catch (Exception e) {
            logger.error("Error during check for wort temperature", e);
        }
    }

    private void checkRoomTempForAlarm() {
        try {
            String roomTemperatureValue = temperatureReader.getRoomTemperature();
            checkViolationAndSendAlarm(roomTemperatureValue, SensorType.room);
        } catch (Exception e) {
            logger.error("Error during check for room temperature", e);
        }
    }

    private void checkViolationAndSendAlarm(String tempValueAsString, SensorType sensorType) {
        Double tempValue = toDouble(tempValueAsString);

        if (tempValue < getLowerTempLimit(sensorType)) {
            lowerViolationsInArow.compute(sensorType, (k,v) -> ++v);
            logger.warn(sensorType + " temperature under lower limit. " +
                    "Violation (" + lowerViolationsInArow.get(sensorType) + "/" + NUMBER_OF_VIOLATIONS + "): " + tempValue);
        } else {
            lowerViolationsInArow.put(sensorType, 0);
        }

        if (tempValue >  getUpperTempLimit(sensorType)) {
            upperViolationsInArow.compute(sensorType, (k,v) -> ++v);
            logger.warn(sensorType + " temperature over upper limit." +
                    "Violation (" + upperViolationsInArow.get(sensorType) + "/" + NUMBER_OF_VIOLATIONS + "): " + tempValue);
        } else {
            upperViolationsInArow.put(sensorType, 0);
        }

        boolean lowerViolation = lowerViolationsInArow.get(sensorType) >= NUMBER_OF_VIOLATIONS;
        boolean upperViolation = upperViolationsInArow.get(sensorType) >= NUMBER_OF_VIOLATIONS;
        if (lowerViolation || upperViolation) {
            logger.warn("sending notification for temperature violations");
            sendMail(sensorType + " temperature " + (lowerViolation ? "under" : "over") + " limit",
                    toParagraph("date:" + LocalDateTime.now().toString()) +
                            toParagraph(sensorType + " temperature " + (lowerViolation ? lowerViolationsInArow.get(sensorType) : upperViolationsInArow.get(sensorType) ) + " times in a row outside range") +
                            toParagraph("Last temp: " + tempValue) +
                            toParagraph("Temperature " + (lowerViolation ? "under" : "over") + " limit value") +
                            toParagraph("Range (" + getLowerTempLimit(sensorType) + " - " + getUpperTempLimit(sensorType) + ")"));
        }
    }

    private Double getLowerTempLimit(SensorType sensorType) {
        return sensorType == SensorType.wort ? WORT_LOWER_TEMPERATURE_LIMIT : ROOM_LOWER_TEMPERATURE_LIMIT;
    }

    private Double getUpperTempLimit(SensorType sensorType) {
        return sensorType == SensorType.wort ? WORT_UPPER_TEMPERATURE_LIMIT : ROOM_UPPER_TEMPERATURE_LIMIT;
    }

    private String toParagraph(String s) {
        return "<p>" + s + "</p>";
    }

    private void sendMail(String subject, String body) {
        try {
            mailSender.sendAlarm(subject, body);
        } catch (MessagingException e) {
            logger.error("Error sending mail" , e);
        }
    }

}
