package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Properties;

import static gg.Constants.TEMPERATURE_SETTINGS;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

class Util {
    private static final Logger logger = LogManager.getLogger(Util.class);

    static Properties getProperties(String arg) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(arg));
        } catch (IOException e) {
            logger.error(e);
            System.exit(-1);
        }
        p.list(System.out);

        return p;
    }

    static String getProperty(Properties p, String key) {
        String value = p.getProperty(key);
        return value!=null?value.trim():null;
    }

    static String getPropertyOrDefault(Properties p, String key, String def) {
        String value = p.getProperty(key, def);
        return value.trim();
    }

    static int getIntegerProperty(Properties properties, String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
    static String toJsDate(LocalDateTime date) {
        int year  = date.getYear();
        int month = date.getMonthValue();
        int day   = date.getDayOfMonth();
        int hour = date.getHour();
        int minute = date.getMinute();
        int second = date.getSecond();

        return "new Date(" + year + ", " + (month - 1) + ", " + day + ", " + hour + ", " + minute + ", " + second + ", 0)";
    }

    static Double toDouble(String tempValueAsString) {
        if(Strings.isNotBlank(tempValueAsString)){
            return Double.parseDouble(tempValueAsString);
        }
        return Double.NaN;
    }

    private static String[] getTemperatureSettingFromProperty(Properties p) {
        String temperatureSettings = getProperty(p, TEMPERATURE_SETTINGS);
        if(temperatureSettings!= null && !temperatureSettings.isEmpty()){
            return temperatureSettings.trim().split(";");
        }
        return new String[0];
    }

    static LinkedHashMap<LocalDateTime, Double> getTemperatureSettings(Properties p) {
        String[] temperatureSettings = getTemperatureSettingFromProperty(p);

        LinkedHashMap<LocalDateTime, Double> settingsTemperature = new LinkedHashMap<>();
        for (int i = 0; i < temperatureSettings.length; i=i+2) {
            LocalDateTime dateTime = LocalDateTime.parse(temperatureSettings[i], ISO_LOCAL_DATE_TIME);
            double temperature = Double.parseDouble(temperatureSettings[i + 1]);
            settingsTemperature.put(dateTime, temperature);
        }
        return settingsTemperature;
    }
}
