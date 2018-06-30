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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static gg.Constants.*;

public class TemperatureCollector extends Thread{

    private static SimpleDateFormat df =  new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"); //TODO use localdatetime
    private static NumberFormat nf =  new DecimalFormat("##.##");

    private static final Logger logger = LogManager.getLogger(TemperatureCollector.class);

    private final String[] sensors;
    private final String sensorsFolder;
    private final Integer numberOfReadToPerform;
    private final String timeBetweenReads;
    private final String outputFilePath;

    public TemperatureCollector(String[] sensors, String sensorsFolder, Integer numberOfReadToPerform, String timeBetweenReads, String outputFilePath) {
        this.sensors = sensors;
        this.sensorsFolder = sensorsFolder;
        this.numberOfReadToPerform = numberOfReadToPerform;
        this.timeBetweenReads = timeBetweenReads;
        this.outputFilePath = outputFilePath;
    }

    static TemperatureCollector build(Properties properties) {
        String[] sensors = properties.getProperty(SENSORS).split(";");

        Integer numberOfReadToPerform = getNumberOfReadToPerform(properties);
        String timeBetweenReads = properties.getProperty(WAIT);
        String outputFilePath = properties.getProperty(TEMPERATURE_OUTPUT_FILE);
        String sensorsFolder = properties.getProperty(SENSORS_FOLDER);

        return new TemperatureCollector(sensors, sensorsFolder, numberOfReadToPerform, timeBetweenReads, outputFilePath);
    }

    @Override
    public void run() {
        int readsDone = 1;
        while (thereAreReadsToDo(readsDone, numberOfReadToPerform)) {
            String now = df.format(new Date());// TODO use iso date
            StringBuilder allSensorLine = new StringBuilder(now);
            for (String sensor : sensors) {
                String temperature = readTemperatureFromFile(buildSensorPath(sensor));
                String line = now + "|" + temperature;
                allSensorLine.append("|").append(temperature).append("|").append(sensor);
                writeTemperatureInFile(sensor + ".txt", line);
            }
            logger.debug("\n" + allSensorLine.toString().replace("|", " "));
            writeTemperatureInFile(outputFilePath, allSensorLine.toString());
            readsDone++;
            pause(timeBetweenReads);
        }
    }

    public static void main(String[] args) {
        build(Util.getProperties(args[0])).run();
    }

    private String buildSensorPath(String sensor) {
        return sensorsFolder + File.separator +  sensor + File.separator  + TEMPERATURE_FILE;
    }

    private static void writeTemperatureInFile(String outputFilePath, String line) {
        writeSingleFile(outputFilePath, line);
    }

    private static void writeSingleFile(String outputFilePath, String line) {
        try {
            Files.write(Paths.get(outputFilePath), Collections.singletonList(line), APPEND, CREATE);
        } catch (IOException e) {
            logger.error("error writing line " +line+" into file : " + outputFilePath, e);
        }
    }

    private static void pause(String secondsToWait) {
        try {
            Thread.sleep(1000 * Integer.parseInt(secondsToWait));
        } catch (InterruptedException e) {
           logger.error(e);
        }
    }

    private static String readTemperatureFromFile(String completeFilePath) {
        String content = getFileContent(completeFilePath);
        checkIfFileIsEmpty(content); //TODO fix this
        String[] split = content.split("t=");
        checkIfFileContainsTemperature(split);
        return nf.format(getTemperature(split[1]));
    }

    private static double getTemperature(String s) {
        double temp = parseDouble(s.trim());
        temp /= 1000;
        return temp;
    }

    private static void checkIfFileContainsTemperature(String[] split) { //TODO handle with optional temporary failures
        if(split.length !=2){
            logger.error("cannot find temperature value");
        }
    }

    private static void checkIfFileIsEmpty(String content) {
        if (content == null || content.isEmpty()) {
            logger.error("file is empty");
        }
    }

    private static String getFileContent(String completeFilePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(completeFilePath)));
        } catch (IOException e) {
            logger.error("Error accessing file " + completeFilePath,e);
            return null;
        }
    }

    private static boolean thereAreReadsToDo(int i, Integer numberOfRead) {
        return numberOfRead == -1 || i <= numberOfRead;
    }

    private static int getNumberOfReadToPerform(Properties properties) {
        return Integer.parseInt((String) properties.getOrDefault(Constants.NUMBER_OF_READ, "-1"));
    }
}
