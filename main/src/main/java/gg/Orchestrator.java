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

        LCD lcd = new LCD();
        lcd.print0("Starting");
        scheduleTemperatureCollector(p, lcd);
        scheduleIOTSender(p);
        scheduleTemperatureAlarm(p);

        ReentrantLock lock = new ReentrantLock();
        scheduleDataProcess(p, lock);
        scheduleFTPUpload(p, lock);

        scheduleController(p);
    }

    private static void scheduleTemperatureAlarm(Properties properties) {
        logger.info("Schedule Temperature alarm Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new TemperatureAlarm(properties);

        logger.info("ITemperature alarm Process. initialDelay= " + 0 + " periodicDelay= " + 60 + " period= " + SECONDS);
        scheduler.scheduleAtFixedRate(task, 0, 60, SECONDS);
    }

    private static void scheduleTemperatureCollector(Properties properties, LCD lcd) {
        logger.info("Schedule temperature collector Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = TemperatureCollector.build(properties, lcd);
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

    private static void scheduleController(Properties properties) {
        logger.info("Schedule Controller");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable controller = new Controller(properties);
        scheduler.schedule(controller, 20, SECONDS);
    }

    private static void checkArguments(String[] args) {
        if(args.length != 2){
            logger.error("mandatory arguments: configPropertyFile credentialPropertyFile");
            System.exit(-1);
        }
        logger.info("Arguments: " + Arrays.asList(args));
    }

    private static Properties mergePropertiesFile(String[] args) {
        String configPropertyFile = args[0];
        String credentialPropertyFile = args[1];

        Properties properties = Util.getProperties(configPropertyFile);
        properties.putAll(Util.getProperties(credentialPropertyFile));
        return properties;
    }

}
