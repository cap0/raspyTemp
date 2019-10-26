package gg;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static gg.util.Constants.TEMPERATURE_FILE;
import static gg.util.Util.formatTemperature;
import static java.lang.Double.parseDouble;

class TemperatureReader implements IReadTemperature{

    private static final Logger logger = LogManager.getLogger(TemperatureReader.class);

    private final String sensorsFolder;
    private final String wortSensorName;
    private final String roomSensorName;

    TemperatureReader(String sensorsFolder, String wortSensorName, String roomSensorName) {
        this.sensorsFolder = sensorsFolder;
        this.wortSensorName = wortSensorName;
        this.roomSensorName = roomSensorName;
    }

    @Override
    public String getRoomTemperature() {
        return readTemperatureForSensor(roomSensorName);
    }

    @Override
    public String getWorthTemperature() {
        return readTemperatureForSensor(wortSensorName);
    }

    private String readTemperatureForSensor(String sensorId) {
        Optional<String> temperature = readTemperatureFromFile(buildSensorPath(sensorId));
        if (temperature.isPresent()) {
            return temperature.get();
        }
        logger.warn("cannot read temperature file for sensor:" + sensorId);
        return "";
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
        return Optional.of(formatTemperature(getTemperature(split[1])));
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

    private String buildSensorPath(String sensorId) {
        return sensorsFolder + File.separator + sensorId + File.separator  + TEMPERATURE_FILE;
    }
}
