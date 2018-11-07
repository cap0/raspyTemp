package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.nio.file.Files.exists;

public class WriteOnUsb implements Runnable {

    private static final String MNT_USB = "/media/usb";
    private String TEMPERATURE_FILE = null;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.ITALY); //TODO handle

    private static final Logger logger = LogManager.getLogger(Util.class);

    public WriteOnUsb(String temperatureOutFileName){
        TEMPERATURE_FILE = temperatureOutFileName;
    }

    public static void main(String args[]) {
        WriteOnUsb r = new WriteOnUsb("");
        Thread t = new Thread(r);
        t.start();
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (Throwable t) {
            logger.fatal(t);
        }
    }

    private void execute() {
        logger.info("Start USB process");
        Path path = Paths.get(MNT_USB);

        while (true) {
            checkIfExistsAndWrite(path);
            sleepSomeTime();
        }
    }

    private void checkIfExistsAndWrite(Path path) {
        if (exists(path)) {
            logger.info("USB is plugged");
            writeFileOnUSB();
        } else {
            logger.debug("USB is not plugged");
        }
    }

    private void writeFileOnUSB() {
        String fileName = MNT_USB + "/data_" + sdf.format(new Date()) + ".txt";
        try {
            byte[] temperatureBytes = Files.readAllBytes(Paths.get(TEMPERATURE_FILE));
            Files.write(Paths.get(fileName), temperatureBytes);
            logger.info("file " + fileName + " has been written");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void sleepSomeTime() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
