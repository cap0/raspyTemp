package gg;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.lang.Double.parseDouble;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static gg.Constants.*;

public class TemperatureCollector extends Thread{

    private static NumberFormat nf =  new DecimalFormat("##.##"); // TODO check locale

    private static final Logger logger = LogManager.getLogger(TemperatureCollector.class);

    private final List<Sensor> sensors;
    private final String sensorsFolder;
    private final Integer numberOfReadToPerform;
    private final String timeBetweenReads;
    private final String outputFilePath;

    public TemperatureCollector(List<Sensor> sensors, String sensorsFolder, Integer numberOfReadToPerform, String timeBetweenReads, String outputFilePath) {
        this.sensors = sensors;
        this.sensorsFolder = sensorsFolder;
        this.numberOfReadToPerform = numberOfReadToPerform;
        this.timeBetweenReads = timeBetweenReads;
        this.outputFilePath = outputFilePath;
    }

    static TemperatureCollector build(Properties properties) {
        List<Sensor> sensors = getSensors(properties);
        Integer numberOfReadToPerform = getNumberOfReadToPerform(properties);
        String timeBetweenReads = properties.getProperty(WAIT);
        String outputFilePath = properties.getProperty(TEMPERATURE_OUTPUT_FILE);
        String sensorsFolder = properties.getProperty(SENSORS_FOLDER);

        return new TemperatureCollector(sensors, sensorsFolder, numberOfReadToPerform, timeBetweenReads, outputFilePath);
    }

    private static List<Sensor> getSensors(Properties properties) {
        List<Sensor> sensors = new ArrayList<>();
        String[] sensorInfoSrt = properties.getProperty(SENSORS).split(";");
        for (String s : sensorInfoSrt) {
            String[] sensorInfo = s.split("\\|");
            sensors.add(new Sensor(sensorInfo[0], sensorInfo[1]));
        }
        return sensors;
    }

    public static void main(String[] args) {
        build(Util.getProperties(args[0])).run();
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
        boolean outputFileExists = Files.exists(Paths.get(outputFilePath));
        if (!outputFileExists) {
            writeHeader();
        }

        int readsDone = 1;
        while (thereAreReadsToDo(readsDone, numberOfReadToPerform)) {
            StringBuilder allSensorLine = new StringBuilder(now());
            for (Sensor sensor : sensors) {
                addSensorTempToRow(allSensorLine, sensor);
            }
            logger.debug("\n" + allSensorLine.toString().replace("|", " "));
            writeTemperatureInFile(outputFilePath, allSensorLine.toString());
            readsDone++;
            pause(timeBetweenReads);
        }
        logger.info("temperature collection procedure completed. Read "+numberOfReadToPerform +" reads");
    }

    private void addSensorTempToRow(StringBuilder allSensorLine, Sensor sensor) {
        Optional<String> temperature = readTemperatureFromFile(buildSensorPath(sensor));
        if (temperature.isPresent()) {
            allSensorLine.append("|").append(temperature.get());
        } else {
            logger.warn("cannot read temperature file for sensor:" + sensor);
        }
    }

    private void writeHeader() {
        StringBuilder header = new StringBuilder("date|");
        for (Sensor s : sensors) {
            header.append(s.encode()).append("|");
        }
        header = header.deleteCharAt(header.lastIndexOf("|"));
        writeTemperatureInFile(outputFilePath, header.toString());
    }

    private String buildSensorPath(Sensor sensor) {
        return sensorsFolder + File.separator +  sensor.id + File.separator  + TEMPERATURE_FILE;
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

    private static Optional<String> readTemperatureFromFile(String completeFilePath) {
        String content = getFileContent(completeFilePath);
        if (StringUtils.isBlank(content)) {
            logger.error("file is empty");
            return Optional.empty();
        }

        String[] split = content.split("t=");
        if(split.length !=2){
            logger.error("cannot find temperature value");
            return Optional.empty();
        }
        return Optional.of(nf.format(getTemperature(split[1])));
    }

    private static double getTemperature(String s) {
        double temp = parseDouble(s.trim());
        temp /= 1000;
        return temp;
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

    private String now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
