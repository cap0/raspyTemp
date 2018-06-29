package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Orchestrator {

    private static final Logger logger = LogManager.getLogger(Orchestrator.class);

    public static void main(String[] args) {
        if(args.length != 2){
            logger.error("mandatory arguments: propertyFile ftpPropertyFile");
            System.exit(-1);
        }

        logger.info("Arguments: " + Arrays.asList(args));
        String propertyFile = args[0];
        String ftpPropertyFile = args[1];

        runTemperatureCollector(propertyFile);

        scheduleDataProcess(Util.getProperties(propertyFile));

        scheduleFTPUpload(Util.getProperties(ftpPropertyFile));

        scheduleGoogleDriveBackup();
    }

    private static void runTemperatureCollector(String propertyFile) {
        logger.info("Fire temperature collector");
        TemperatureCollector temperatureCollector = TemperatureCollector.build(propertyFile);
        temperatureCollector.start();
    }

    private static void scheduleDataProcess(Properties properties) {
        logger.info("Schedule data Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new ProcessTemperatureData(properties);
        int initialDelay = 10; //TODO property file
        int periodicDelay = 10;

        scheduler.scheduleAtFixedRate(task, initialDelay, periodicDelay, TimeUnit.SECONDS);
    }

    private static void scheduleFTPUpload(Properties properties) {
        logger.info("Schedule FTP Upload");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable ftpUploadTask = new FTPUploadFile(properties);
        int initialDelay = 10; //TODO property file
        int periodicDelay = 10;

        scheduler.scheduleAtFixedRate(ftpUploadTask, initialDelay, periodicDelay, TimeUnit.SECONDS);
    }

    private static void scheduleGoogleDriveBackup() {
        logger.info("Schedule Google Drive Backup");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable googleDriveTask = new GoogleDriveHelper();
        int initialDelay = 1; //TODO property file
        int periodicDelay = 5;

        scheduler.scheduleAtFixedRate(googleDriveTask, initialDelay, periodicDelay, TimeUnit.HOURS);
    }

}
