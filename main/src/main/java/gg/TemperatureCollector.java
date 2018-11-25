package gg;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static gg.Constants.*;
import static java.lang.Double.parseDouble;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class TemperatureCollector extends Thread{

    private static NumberFormat nf =  new DecimalFormat("##.##");

    private static final Logger logger = LogManager.getLogger(TemperatureCollector.class);

    private final String sensorsFolder;
    private final String outputFilePath;
    private final String roomSensorName;
    private final String wortSensorName;

    public TemperatureCollector(String roomSensorName, String wortSensorName, String sensorsFolder, String outputFilePath) {
        this.roomSensorName = roomSensorName;
        this.wortSensorName = wortSensorName;
        this.sensorsFolder = sensorsFolder;
        this.outputFilePath = outputFilePath;
    }

    static TemperatureCollector build(Properties properties) {
        String roomSensorName = properties.getProperty(ROOM_SENSOR);
        String wortSensorName = properties.getProperty(WORT_SENSOR);
        String outputFilePath = properties.getProperty(TEMPERATURE_OUTPUT_FILE);
        String sensorsFolder = properties.getProperty(SENSORS_FOLDER);

        return new TemperatureCollector(roomSensorName, wortSensorName, sensorsFolder, outputFilePath);
    }

    public static void main(String[] args) {
        build(Util.getProperties(args[0])).execute();
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
        logger.info("Start collecting temperature");
        boolean outputFileExists = Paths.get(outputFilePath).toFile().exists();
        if (!outputFileExists) {
            logger.info("Writing Header");
            writeHeader();
        }

        String roomTemperatureValue = readTemperatureForSensor(roomSensorName);
        String wortTemperatureValue = readTemperatureForSensor(wortSensorName);

        String line = now() + "|" + roomTemperatureValue + "|" + wortTemperatureValue;

        logger.info(line);
        writeTemperatureInFile(outputFilePath, line);
    }

    private String readTemperatureForSensor(String sensorId) {
        Optional<String> temperature = readTemperatureFromFile(buildSensorPath(sensorId));
        if (temperature.isPresent()) {
            return temperature.get();
        }
        logger.warn("cannot read temperature file for sensor:" + sensorId);
        return "";
    }

    private void writeHeader() {
        writeTemperatureInFile(outputFilePath, "date|room|wort");
    }

    private String buildSensorPath(String sensorId) {
        return sensorsFolder + File.separator + sensorId + File.separator  + TEMPERATURE_FILE;
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

    private String now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
