package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static gg.Constants.ENABLE_GOOGLE_DRIVE;
import static gg.Constants.ENABLE_USB_DRIVE;
import static gg.Util.getIntegerProperty;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Orchestrator {

    private static final Logger logger = LogManager.getLogger(Orchestrator.class);

    public static void main(String[] args) {
        checkArguments(args);
        Properties properties = mergePropertiesFile(args);

        runTemperatureCollector(properties);
        scheduleDataProcess(properties);
        scheduleFTPUpload(properties);
        scheduleGoogleDriveBackup(properties);
        runWriteToUsb(properties);
    }

    private static void runWriteToUsb(Properties properties) {
        Boolean enableUsbDrive = Boolean.valueOf(properties.getProperty(ENABLE_USB_DRIVE, "false"));
        if(enableUsbDrive) {
            String temperatureOutFileName = properties.getProperty(Constants.TEMPERATURE_OUTPUT_FILE);
            new WriteOnUsb(temperatureOutFileName).run();
        }
    }

    private static void runTemperatureCollector(Properties properties) {
        logger.info("Starting temperature collector");
        TemperatureCollector temperatureCollector = TemperatureCollector.build(properties);
        temperatureCollector.start();
    }

    private static void scheduleDataProcess(Properties properties) {
        logger.info("Schedule data Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new ProcessTemperatureData(properties);
        int initialDelay = getIntegerProperty(properties, "process.initialDelay");
        int periodicDelay = getIntegerProperty(properties, "process.periodicDelay");

        logger.info("Data Process. initialDelay= " + initialDelay + " periodicDelay= " + periodicDelay + " period= " + SECONDS);
        scheduler.scheduleAtFixedRate(task, initialDelay, periodicDelay, SECONDS);
    }

    private static void scheduleFTPUpload(Properties properties) {
        logger.info("Schedule FTP Upload");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable ftpUploadTask = new FTPUploadFile(properties);
        int initialDelay = getIntegerProperty(properties, "ftp.initialDelay");
        int periodicDelay = getIntegerProperty(properties, "ftp.periodicDelay");

        scheduler.scheduleAtFixedRate(ftpUploadTask, initialDelay, periodicDelay, MINUTES);
    }

    private static void scheduleGoogleDriveBackup(Properties properties) {
        Boolean enableGoogleDrive = Boolean.valueOf(properties.getProperty(ENABLE_GOOGLE_DRIVE));
        if (enableGoogleDrive) {
            logger.info("Schedule Google Drive Backup");
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable googleDriveTask = new GoogleDriveHelper(properties);
            int initialDelay = getIntegerProperty(properties, "driveBackup.initialDelay");
            int periodicDelay = getIntegerProperty(properties, "driveBackup.periodicDelay");

            scheduler.scheduleAtFixedRate(googleDriveTask, initialDelay, periodicDelay, MINUTES);
        }else{
            logger.info("Google Drive Backup disabled");
        }
    }

    private static void checkArguments(String[] args) {
        if(args.length != 2){
            logger.error("mandatory arguments: propertyFile ftpPropertyFile");
            System.exit(-1);
        }
        logger.info("Arguments: " + Arrays.asList(args));
    }

    private static Properties mergePropertiesFile(String[] args) {
        String propertyFile = args[0];
        String ftpPropertyFile = args[1];

        Properties properties = Util.getProperties(propertyFile);
        properties.putAll(Util.getProperties(ftpPropertyFile));
        return properties;
    }

}
