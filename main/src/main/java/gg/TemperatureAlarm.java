package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.util.Properties;

import static gg.Constants.*;

public class TemperatureAlarm implements Runnable {
    private static final Logger logger = LogManager.getLogger(TemperatureAlarm.class);

    private static final int NUMBER_OF_VIOLATIONS = 3;
    private static final double LOWER_TEMPERATURE_LIMIT = 0;
    private static final double UPPER_TEMPERATURE_LIMIT = 25;

    private final String roomSensorName;
    private final String wortSensorName;
    private int lowerViolationsInArow = 0;
    private int upperViolationsInArow = 0;

    private final ReadTemperature temperatureReader;
    private final GMailSender mailSender;

    TemperatureAlarm(Properties p) {
        roomSensorName = p.getProperty(ROOM_SENSOR);
        wortSensorName = p.getProperty(WORT_SENSOR);

        temperatureReader = new ReadTemperature(p.getProperty(SENSORS_FOLDER));
        mailSender = new GMailSender(p);
    }

    @Override
    public void run() {
        logger.debug("checking temperature for alarm");
        checkWortTempForAlarm();
        checkRoomTempForAlarm();
    }

    private void checkWortTempForAlarm() {
        try {
            String wortTemperatureValue = temperatureReader.readTemperatureForSensor(wortSensorName);
            checkViolationAndSendAlarm(wortTemperatureValue, "Wort");
        } catch (Exception e) {
            logger.error("Error during check for wort temperature", e);
        }
    }

    private void checkRoomTempForAlarm() {
        try {
            String roomTemperatureValue = temperatureReader.readTemperatureForSensor(roomSensorName);
            checkViolationAndSendAlarm(roomTemperatureValue, "Room");
        } catch (Exception e) {
            logger.error("Error during check for room temperature", e);
        }
    }

    private void checkViolationAndSendAlarm(String tempValueAsString, String sensorType) {
        Double tempValue = toDouble(tempValueAsString);

        if (tempValue < LOWER_TEMPERATURE_LIMIT) {
            lowerViolationsInArow++;
            logger.warn(sensorType + " temperature under lower limit. " +
                    "Violation (" + lowerViolationsInArow + "/" + NUMBER_OF_VIOLATIONS + "): " + tempValue);
        } else {
            lowerViolationsInArow = 0;
        }

        if (tempValue > UPPER_TEMPERATURE_LIMIT) {
            upperViolationsInArow++;
            logger.warn(sensorType + " temperature over upper limit." +
                    "Violation (" + upperViolationsInArow + "/" + NUMBER_OF_VIOLATIONS + "): " + tempValue);
        } else {
            upperViolationsInArow = 0;
        }

        boolean lowerViolation = lowerViolationsInArow >= NUMBER_OF_VIOLATIONS;
        boolean upperViolation = upperViolationsInArow >= NUMBER_OF_VIOLATIONS;
        if (lowerViolation || upperViolation) {
            logger.warn("sending notification for temperature violations");
            sendMail(sensorType + " temperature " + (lowerViolation ? "under" : "over") + " limit",
                    toParagraph("date:" + LocalDateTime.now().toString()) +
                            toParagraph(sensorType + " temperature " + (lowerViolation ? lowerViolationsInArow : upperViolationsInArow) + " times in a row outside range") +
                            toParagraph("Last temp: " + tempValue) +
                            toParagraph("Temperature " + (lowerViolation ? "under" : "over") + " limit value") +
                            toParagraph("Range (" + LOWER_TEMPERATURE_LIMIT + " - " + UPPER_TEMPERATURE_LIMIT + ")"));
        }
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

    private Double toDouble(String tempValueAsString) {
        if(Strings.isNotBlank(tempValueAsString)){
            return Double.parseDouble(tempValueAsString);
        }
        return Double.NaN;
    }
}
