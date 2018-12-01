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
    static final String TEMPERATURE_SETTINGS = "temperatureSettings";
    static final String GENERATE_XLSX_FILE = "generateXlsxFile";
    static final String MIN_ALLOWED_TEMP = "minAllowedTemp";
    static final String MAX_ALLOWED_TEMP = "maxAllowedTemp";
    static final String XLSX_OUTPUT_FILE = "xlsxOutputFile";
    static final String SERIES_AGGREGATION_FACTOR = "seriesAggregationFactor";

    //FTP
    static final String FTP_HOST = "ftpHost";
    static final String FTP_PORT = "ftpPort";
    static final String FTP_USER = "ftpUser";
    static final String FTP_PASS = "ftpPass";
    static final String HTML_PAGE_NAME = "htmlPageName";

    //Google
    static final String ENABLE_GOOGLE_DRIVE = "enableGoogleDrive";
    static final String FILE_NAME_ON_GOOGLE_DRIVE = "fileNameOnGoogleDrive";
    static final String GOOGLE_DRIVE_FILE_PATH = "googleCredentialFilePath";

    //USB
    static final String ENABLE_USB_DRIVE = "enableUsbDrive";

    //IOT
    static final String WRITE_KEY = "writeKey";

    //TIMER DELAYS
    static final String PROCESSES_PERIODIC_DELAY = "process.periodicDelay";


}