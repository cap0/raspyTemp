package gg;

class Constants {
    //Used to read temperature from file
    static final String ROOM_SENSOR = "roomSensor";
    static final String WORT_SENSOR = "wortSensor";
    static final String SENSORS_FOLDER = "sensorsFolder";
    static final String TEMPERATURE_FILE = "w1_slave";
    static final String TEMPERATURE_OUTPUT_FILE = "temperatureOutputFile";
    static final String TEMPERATURE_PROCESSED_OUTPUT_FILE = "temperatureProcessedOutputFile";
    static final String DATE_PATTERN = "datePattern";

    //Used to process data
    static final String START_DATE = "startDate";
    static final String END_DATE = "endDate";
    static final String TEMPERATURE_SETTINGS_FILE_PATH = "temperatureSettingsFilePath";
    static final String MIN_ALLOWED_TEMP = "minAllowedTemp";
    static final String MAX_ALLOWED_TEMP = "maxAllowedTemp";
    static final String SERIES_AGGREGATION_FACTOR = "seriesAggregationFactor";

    //FTP
    static final String FTP_HOST = "ftp.host";
    static final String FTP_PORT = "ftp.port";
    static final String FTP_USER = "ftp.user";
    static final String FTP_PASS = "ftp.pass";
    static final String HTML_PAGE_NAME = "htmlPageName";

    //IOT
    static final String IOT_WRITE_KEY = "iot.writeKey";

    //TIMER DELAYS
    static final String PROCESSES_PERIODIC_DELAY = "process.periodicDelay";

    //MAIL
    static final String MAIL_USERNAME = "mail.username";
    static final String MAIL_PASSWORD = "mail.password";
    static final String MAIL_RECEIVER = "mail.receiver";

    static final String CTRL_DELTA_TEMP = "ctrl.delta.temp";
}