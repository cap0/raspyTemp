package gg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.nio.file.Files.exists;

public class WriteOnUsb implements Runnable {

    private static final String MNT_USB = "/mnt/usb";
    private static final String TEMPERATURE_FILE = "/path/test.txt";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.ITALY);

    public static void main(String args[]) {
        WriteOnUsb r = new WriteOnUsb();
        Thread t = new Thread(r);
        t.start();
    }

    @Override
    public void run() {
        System.out.println("Start.");
        Path path = Paths.get(MNT_USB);

        while (true) {
            checkIfExistsAndWrite(path);
            sleepSomeTime();
        }
    }

    private void checkIfExistsAndWrite(Path path) {
        if (exists(path)) {
            System.out.println("USB is plugged");
            writeFileOnUSB();
        } else {
            System.out.println("USB is not plugged");
        }
    }

    private void writeFileOnUSB() {
        String fileName = MNT_USB + "/data_" + sdf.format(new Date()) + ".txt";
        try {
            byte[] temperatureBytes = Files.readAllBytes(Paths.get(TEMPERATURE_FILE));
            Files.write(Paths.get(fileName), temperatureBytes);
            System.out.println("file " + fileName + " has been written");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sleepSomeTime() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
