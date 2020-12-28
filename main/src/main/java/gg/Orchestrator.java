package gg;

import gg.GPIO.GPIOController;
import gg.GPIO.GPIOControllerMock;
import gg.GPIO.IGPIOController;
import gg.LCD.ILCD;
import gg.LCD.LCD;
import gg.LCD.LCDMock;
import gg.TemperatureSetting.TemperatureSettingsFileHandler;
import gg.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import static gg.util.PropertyUtil.*;
import static gg.util.Util.getIntegerProperty;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Orchestrator {

    private static final Logger logger = LogManager.getLogger(Orchestrator.class);

    public static void main(String[] args) {
        Orchestrator o = new Orchestrator();
        o.init(args);
    }

    public void init(String[] args) {
        logger.info("all starts here...");
        checkArguments(args);
        Properties p = mergePropertiesFile(args);

        createSettingsFileIfMissing(p);
        boolean isMock = isMock(p);

        ILCD lcd = createLCD(isMock);
        lcd.print("starting...","");
        IGPIOController gpioCtrl = createGPIOController(isMock);

        scheduleTemperatureCollector(p, gpioCtrl);
        scheduleIOTSender(p);
        scheduleBrewfatherConnector(p);
        scheduleTemperatureAlarm(p);

        ReentrantLock lock = new ReentrantLock();
        scheduleDataProcess(p, lock);
        scheduleFTPUpload(p, lock);

        ConnectionChecker connCheck = scheduleConnectionChecker();

        scheduleController(p, connCheck, lcd, gpioCtrl);

        startHttpServer(p);
    }

    private static boolean isMock(Properties p) {
        return Boolean.parseBoolean(p.getProperty("mock", "false"));
    }

    private static ILCD createLCD(boolean isMock) {
        return isMock ? new LCDMock() : new LCD();
    }

    private static IGPIOController createGPIOController(boolean isMock) {
        return isMock ? new GPIOControllerMock() : new GPIOController();
    }

    private static void createSettingsFileIfMissing(Properties p) {
        String temperatureSettingsPath = getTemperatureSettingsPath(p);
        if (!Files.exists(Paths.get(temperatureSettingsPath))) {
            logger.info("Temperature Settings File not detected in: " + temperatureSettingsPath);
            try {
                TemperatureSettingsFileHandler ts = new TemperatureSettingsFileHandler(temperatureSettingsPath);
                ts.init();
            } catch (IOException e) {
                logger.error("cannot create temp file");
            }
        }
    }

    private static void startHttpServer(Properties p) {
        MyHttpServer myHttpServer = new MyHttpServer(p);
        myHttpServer.startHttpServer();
    }

    private static ConnectionChecker scheduleConnectionChecker() {
        logger.info("Schedule Connection checker");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ConnectionChecker task = new ConnectionChecker();

        logger.info("Connection Checker Process. initialDelay= " + 0 + " periodicDelay= " + 60 + " period= " + SECONDS);
        scheduler.scheduleAtFixedRate(task, 0, 60, SECONDS);
        return task;
    }

    private static void scheduleTemperatureAlarm(Properties properties) {
        logger.info("Schedule Temperature alarm Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new TemperatureAlarm(properties);

        logger.info("ITemperature alarm Process. initialDelay= " + 0 + " periodicDelay= " + 60 + " period= " + SECONDS);
        scheduler.scheduleAtFixedRate(task, 0, 60, SECONDS);
    }

    private static void scheduleTemperatureCollector(Properties properties, IGPIOController gpioCtrl) {
        logger.info("Schedule temperature collector Process");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = TemperatureCollector.build(properties, gpioCtrl);
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

    private static void scheduleBrewfatherConnector(Properties properties) {
        logger.info("Schedule Brewfather connector");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new BrewfatherConnector(properties);

        logger.info("Brewfather connector Process. initialDelay= " + 0 + " periodicDelay= " + 16 + " period= " + MINUTES);
        scheduler.scheduleAtFixedRate(task, 0, 16, MINUTES);
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

    private static void scheduleController(Properties properties, ConnectionChecker connCheck, ILCD lcd, IGPIOController gpioCtrl) {
        logger.info("Schedule Controller");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable controller = new Controller(properties, connCheck, lcd, gpioCtrl);
        scheduler.scheduleAtFixedRate(controller, 10,10, SECONDS);
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
