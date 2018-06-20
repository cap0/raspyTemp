package gg;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Double.parseDouble;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class Main { //TODO add logging system

    private static String SENSORS ="sensors";
    private static String WAIT ="wait";
    private static String NUMBER_OF_READ ="numberOfRead";

    private static String SENSORS_FOLDER = "/sys/bus/w1/devices/";
    private static String TEMPERATURE_FILE = "/w1_slave";

    private static SimpleDateFormat df =  new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"); //TODO use localdatetime
    private static NumberFormat nf =  new DecimalFormat("##.##");

    public static void main(String[] args) {
        System.out.println("Start: " + args[0]);

        Properties properties = getProperties(args[0]);
        String[] sensors = properties.getProperty(SENSORS).split(";");

        int i = 1;
        Integer numberOfRead = Integer.parseInt((String) properties.getOrDefault(NUMBER_OF_READ, "-1"));
        while(numberOfRead == -1 || i <= numberOfRead ) {
            String now =df.format(new Date());
            StringBuilder allSensorLine= new StringBuilder(now);
            for (String sensor : sensors) {
                String temperature = nf.format(readTemperatureFromFile(buildSensorPath(sensor)));
                String line = now + "|" + temperature;
                allSensorLine.append("|").append(temperature).append("|").append(sensor);
                writeTemperatureInFile(sensor+".txt", line);
            }
            System.out.println("\n" + allSensorLine.toString().replace("|" , " " ));
            writeTemperatureInFile("all.txt", allSensorLine.toString() ); //TODO make this configurable
            pause(properties.getProperty(WAIT));
            i++;
        }
    }

    private static String buildSensorPath(String sensor) {
        return SENSORS_FOLDER+sensor+TEMPERATURE_FILE;
    }

    public static Properties getProperties(String arg) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(arg));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        p.list(System.out);

        return p;
    }

    private static void writeTemperatureInFile(String outputFilePath, String line) {
        writeSingleFile(outputFilePath, line);
    }

    private static void writeSingleFile(String outputFilePath, String line) {
        try {
            Files.write(Paths.get(outputFilePath), Collections.singletonList(line), APPEND, CREATE);
        } catch (IOException e) {
            System.err.println("error writing line " +line+" into file : " + outputFilePath);
            e.printStackTrace();
        }
    }

    private static void pause(String secondsToWait) {
        try {
            Thread.sleep(1000 * Integer.parseInt(secondsToWait));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static double readTemperatureFromFile(String completeFilePath) {
        String content = getFileContent(completeFilePath);
        checkIfFileIsEmpty(content);
        String[] split = content.split("t=");
        checkIfFileContainsTemperature(split);
        return getTemperature(split[1]);
    }

    private static double getTemperature(String s) {
        double temp = parseDouble(s.trim());
        temp /= 1000;
        return temp;
    }

    private static void checkIfFileContainsTemperature(String[] split) {
        if(split.length !=2){
            System.err.println("cannot find temperature value");
            System.exit(-1);
        }
    }

    private static void checkIfFileIsEmpty(String content) {
        if (content == null || content.isEmpty()) {
            System.err.println("file is empty");
            System.exit(-1);
        }
    }

    private static String getFileContent(String completeFilePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(completeFilePath)));
        } catch (IOException e) {
            System.err.println("Error accessing file " + completeFilePath);
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }
}
