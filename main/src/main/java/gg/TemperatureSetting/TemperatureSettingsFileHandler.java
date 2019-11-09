package gg.TemperatureSetting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.stream.Collectors.toList;

public class TemperatureSettingsFileHandler implements ITemperatureSettingsSourceHandler{
    private static final Logger logger = LogManager.getLogger(TemperatureSettingsFileHandler.class);

    private String filePath;

    public TemperatureSettingsFileHandler(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public List<String> readLineSettings() {
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            return lines.collect(toList());
        } catch (IOException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void backupAndWriteFile(Set<TemperatureRangeSetting> v) throws IOException {
        Path destination = Paths.get(filePath + "." + System.currentTimeMillis() + ".bkp");
        Path source = Paths.get(filePath);
        Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        Files.write(Paths.get(filePath), buildRows(v), CREATE);
    }

    private List<String> buildRows(Set<TemperatureRangeSetting> v) {
        return  v.stream().map(TemperatureRangeSetting::formatForFile).collect(toList());
    }
}
