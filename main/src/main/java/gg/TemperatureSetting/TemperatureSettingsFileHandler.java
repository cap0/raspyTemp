package gg.TemperatureSetting;

import org.apache.commons.lang3.Range;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
        write(v);
    }

    @Override
    public void init() throws IOException {
        Set<TemperatureRangeSetting> v = new TreeSet<>();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime endFirstRange = now.plusDays(5);
        v.add(new TemperatureRangeSetting(Range.between(now, endFirstRange), 17D));

        LocalDateTime endSecondRange = endFirstRange.plusDays(1);
        v.add(new TemperatureRangeSetting(Range.between(endFirstRange, endSecondRange), 18D));

        LocalDateTime endThirdRange = endSecondRange.plusDays(1);
        v.add(new TemperatureRangeSetting(Range.between(endSecondRange, endThirdRange), 19D));

        LocalDateTime endForthRange = endThirdRange.plusDays(1);
        v.add(new TemperatureRangeSetting(Range.between(endThirdRange, endForthRange), 20D));

        v.add(new TemperatureRangeSetting(Range.between(endThirdRange, endForthRange.plusDays(1)), 21D));

        write(v);
    }

    private Path write(Set<TemperatureRangeSetting> v) throws IOException {
        return Files.write(Paths.get(filePath), buildRows(v), CREATE);
    }

    private static List<String> buildRows(Set<TemperatureRangeSetting> v) {
        return  v.stream().map(TemperatureRangeSetting::formatForCSVFile).collect(toList());
    }
}

