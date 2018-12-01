package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import static gg.Constants.*;
import static gg.Util.getIntegerProperty;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Orchestrator {

    private static final Logger logger = LogManager.getLogger(Orchestrator.class);

    public static void main(String[] args) {
        checkArguments(args);
        Properties p = mergePropertiesFile(args);

        scheduleTemperatureCollector(p);
        scheduleIOTSender(p);

        ReentrantLock lock = new ReentrantLock();
        scheduleDataProcess(p, lock);
        scheduleFTPUpload(p, lock);

        scheduleGoogleDriveBackup(p);
        runWriteToUsb(p);
    }

    private static void scheduleTemperatureCollector(Properties properties) {
        logger.info("Schedule temperature collector Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = TemperatureCollector.build(properties);
        int periodicDelay = getIntegerProperty(properties, PROCESSES_PERIODIC_DELAY);

        logger.info("Temperature collector Process. initialDelay= " + 0 + " periodicDelay= " + periodicDelay + " period= " + SECONDS);
        scheduler.scheduleAtFixedRate(task, 0, periodicDelay, SECONDS);
    }

    private static void scheduleIOTSender(Properties properties) {
        logger.info("Schedule IOT sender Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new IotSender(properties);
        int periodicDelay = getIntegerProperty(properties, PROCESSES_PERIODIC_DELAY);

        logger.info("IOT sender Process. initialDelay= " + 0 + " periodicDelay= " + periodicDelay + " period= " + SECONDS);
        scheduler.scheduleAtFixedRate(task, 0, periodicDelay, SECONDS);
    }

    private static void scheduleDataProcess(Properties properties, ReentrantLock lock) {
        logger.info("Schedule data Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new ProcessTemperatureData(properties, lock);
        int initialDelay = getIntegerProperty(properties, "process.initialDelay");
        int periodicDelay = getIntegerProperty(properties, PROCESSES_PERIODIC_DELAY);

        logger.info("Data Process. initialDelay= " + initialDelay + " periodicDelay= " + periodicDelay + " period= " + SECONDS);
        scheduler.scheduleAtFixedRate(task, initialDelay, periodicDelay, SECONDS);
    }

    private static void scheduleFTPUpload(Properties properties, ReentrantLock lock) {
        logger.info("Schedule FTP Upload");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable ftpUploadTask = new FTPUploadFile(properties, lock);
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

    private static void runWriteToUsb(Properties properties) {
        Boolean enableUsbDrive = Boolean.valueOf(properties.getProperty(ENABLE_USB_DRIVE, "false"));
        if(enableUsbDrive) {
            String temperatureOutFileName = properties.getProperty(Constants.TEMPERATURE_OUTPUT_FILE);
            new WriteOnUsb(temperatureOutFileName).run();
        } else{
            logger.info("Write on USB is disabled");
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
