package gg.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Properties;

public class Util {
    private static final Logger logger = LogManager.getLogger(Util.class);

    public static Properties getProperties(String arg) {
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

    public static String getProperty(Properties p, String key) {
        String value = p.getProperty(key);
        return value!=null?value.trim():null;
    }

    public static String getPropertyOrDefault(Properties p, String key, String def) {
        String value = p.getProperty(key, def);
        return value.trim();
    }

    public static int getIntegerProperty(Properties properties, String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
    public static String toJsDate(LocalDateTime date) {
        int year  = date.getYear();
        int month = date.getMonthValue();
        int day   = date.getDayOfMonth();
        int hour = date.getHour();
        int minute = date.getMinute();
        int second = date.getSecond();

        return "new Date(" + year + ", " + (month - 1) + ", " + day + ", " + hour + ", " + minute + ", " + second + ", 0)";
    }

    public static Double toDouble(String tempValueAsString) {
        if(Strings.isNotBlank(tempValueAsString)){
            return Double.parseDouble(tempValueAsString);
        }
        return Double.NaN;
    }

    private static NumberFormat nf =  new DecimalFormat("##.#");
    public static String formatTemperature(double temperature) {
        return nf.format(temperature);
    }
}