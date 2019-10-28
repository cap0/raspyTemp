package gg.util;

import java.util.Properties;

import static gg.util.Util.getProperty;

public class PropertyUtil {
    //Used to read temperature from file
    public static final String ROOM_SENSOR = "roomSensor";
    public static final String WORT_SENSOR = "wortSensor";
    public static final String SENSORS_FOLDER = "sensorsFolder";
    public static final String TEMPERATURE_FILE = "w1_slave";
    public static final String TEMPERATURE_OUTPUT_FILE = "temperatureOutputFile";
    public static final String TEMPERATURE_PROCESSED_OUTPUT_FILE = "temperatureProcessedOutputFile";
    public static final String DATE_PATTERN = "datePattern";

    //Used to process data
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String MIN_ALLOWED_TEMP = "minAllowedTemp";
    public static final String MAX_ALLOWED_TEMP = "maxAllowedTemp";
    public static final String SERIES_AGGREGATION_FACTOR = "seriesAggregationFactor";

    //FTP
    public static final String FTP_HOST = "ftp.host";
    public static final String FTP_PORT = "ftp.port";
    public static final String FTP_USER = "ftp.user";
    public static final String FTP_PASS = "ftp.pass";
    public static final String HTML_PAGE_NAME = "htmlPageName";

    //IOT
    public static final String IOT_WRITE_KEY = "iot.writeKey";

    //TIMER DELAYS
    public static final String PROCESSES_PERIODIC_DELAY = "process.periodicDelay";

    //MAIL
    public static final String MAIL_USERNAME = "mail.username";
    public static final String MAIL_PASSWORD = "mail.password";
    public static final String MAIL_RECEIVER = "mail.receiver";

    //CONTROLLER
    public static final String CTRL_DELTA_TEMP = "ctrl.delta.temp";
    public static final String CTRL_DELTA_TEMP_ACTIVE = "ctrl.delta.temp.active";


    private static final String TEMPERATURE_SETTINGS_FILE_PATH = "temperatureSettingsFilePath";
    public static String getTemperatureSettingsPath(Properties p) {
        return getProperty(p, TEMPERATURE_SETTINGS_FILE_PATH);
    }

    private static final String TELEGRAM_BOT_API_KEY = "telegram.bot.api.key";
    public static String getTelegramBotApiKey(Properties p) {
        return getProperty(p, TELEGRAM_BOT_API_KEY);
    }

    private static final String TELEGRAM_CHANNEL_NAME = "telegram.channel.name";
    public static String getTelegramChannelName(Properties p) {
        return getProperty(p, TELEGRAM_CHANNEL_NAME);
    }
}