package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;

import static gg.Constants.*;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class TemperatureCollector extends Thread{


    private static final Logger logger = LogManager.getLogger(TemperatureCollector.class);

    private final String outputFilePath;
    private final TemperatureReader temperatureReader;

    private TemperatureCollector(String roomSensorName, String wortSensorName, String sensorsFolder, String outputFilePath) {
        this.outputFilePath = outputFilePath;
        this.temperatureReader = new TemperatureReader(sensorsFolder, wortSensorName, roomSensorName);
    }

    static TemperatureCollector build(Properties p) {
        String roomSensorName = p.getProperty(ROOM_SENSOR);
        String wortSensorName = p.getProperty(WORT_SENSOR);
        String outputFilePath = p.getProperty(TEMPERATURE_OUTPUT_FILE);
        String sensorsFolder = p.getProperty(SENSORS_FOLDER);

        return new TemperatureCollector(roomSensorName, wortSensorName, sensorsFolder, outputFilePath);
    }

    public static void main(String[] args) {
        build(Util.getProperties(args[0])).execute();
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (Exception t) {
            logger.fatal(t);
        }
    }

    private void execute() {
        logger.debug("Start collecting temperature");
        boolean outputFileExists = Paths.get(outputFilePath).toFile().exists();
        if (!outputFileExists) {
            logger.info("Writing Header");
            writeHeader();
        }

        String roomTemperatureValue = temperatureReader.getRoomTemperature();
        String wortTemperatureValue = temperatureReader.getWorthTemperature();

        String line = now() + "|" + roomTemperatureValue + "|" + wortTemperatureValue;

        logger.info(line);
        writeTemperatureInFile(outputFilePath, line);
    }

    private void writeHeader() {
        writeTemperatureInFile(outputFilePath, "date|room|wort");
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

    private String now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
