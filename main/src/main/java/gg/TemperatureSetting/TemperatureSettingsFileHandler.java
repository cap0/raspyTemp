package gg.TemperatureSetting;

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
import static org.apache.commons.lang3.Range.*;

public class TemperatureSettingsFileHandler implements ITemperatureSettingsSourceHandler{
    private static final Logger logger = LogManager.getLogger(TemperatureSettingsFileHandler.class);
    public static final double DIACETIL_REST = 20D;

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
        logger.info("Creation of a default temperature settings file");
        Set<TemperatureRangeSetting> v = generateAleTemperatureSettingsFile();
        write(v);
    }

    public static void main(String ... args) throws IOException {

        String path = args[0];
        TemperatureSettingsFileHandler t = new TemperatureSettingsFileHandler(path);
        LocalDateTime d = LocalDateTime.of(2020, 1, 3, 23, 15);
        Set<TemperatureRangeSetting> r = t.generateLagerTemperatureSettingsFile(d);
        t.write(r);

    }

    public Set<TemperatureRangeSetting> generateLagerTemperatureSettingsFile(LocalDateTime startDate) {
        //FAST LAGER: http://brulosophy.com/methods/lager-method/
        //http://www.rovidbeer.it/metodo-fast-lager/
        Set<TemperatureRangeSetting> v = new TreeSet<>();

        //first fermentation days 5-8
        LocalDateTime s = startDate.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime currentDate = s.plusDays(7); // this is an estimation 5-8
        double currentTemp = 10D;
        v.add(new TemperatureRangeSetting(between(s, currentDate), currentTemp)); // low temp fermentation

        //+2° in 12h = +0.5 in 3h to reach 20°
        while (currentTemp< DIACETIL_REST) {
            currentTemp += 0.5;
            LocalDateTime newEnd = currentDate.plusHours(3);
            v.add(new TemperatureRangeSetting(between(currentDate, newEnd), currentTemp));
            currentDate = newEnd;
        }

        //5 days @ 18°
        LocalDateTime diacetilRestEnd = currentDate.plusDays(5);
        currentTemp = DIACETIL_REST;
        v.add(new TemperatureRangeSetting(between(currentDate, diacetilRestEnd), currentTemp));
        currentDate = diacetilRestEnd;
        currentDate = rampDown(v, currentDate, currentTemp);

        v.add(new TemperatureRangeSetting(between(currentDate, currentDate.plusDays(10)), 2D));
        return v;
    }

    public LocalDateTime rampDown(Set<TemperatureRangeSetting> v, LocalDateTime currentDate, double currentTemp) {
        // -4° in 12h 1° each 3h to reach 2
        while (currentTemp>2D) {
            currentTemp -= 1;
            LocalDateTime newEnd = currentDate.plusHours(3);
            v.add(new TemperatureRangeSetting(between(currentDate, newEnd), currentTemp));
            currentDate = newEnd;
        }
        return currentDate;
    }

    private Set<TemperatureRangeSetting> generateAleTemperatureSettingsFile() {
        Set<TemperatureRangeSetting> v = new TreeSet<>();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime endFirstRange = now.plusDays(5);
        v.add(new TemperatureRangeSetting(between(now, endFirstRange), 17D));

        LocalDateTime endSecondRange = endFirstRange.plusDays(1);
        v.add(new TemperatureRangeSetting(between(endFirstRange, endSecondRange), 18D));

        LocalDateTime endThirdRange = endSecondRange.plusDays(1);
        v.add(new TemperatureRangeSetting(between(endSecondRange, endThirdRange), 19D));

        LocalDateTime endForthRange = endThirdRange.plusDays(1);
        v.add(new TemperatureRangeSetting(between(endThirdRange, endForthRange), 20D));

        v.add(new TemperatureRangeSetting(between(endForthRange, endForthRange.plusDays(1)), 21D));
        return v;
    }

    public Path write(Set<TemperatureRangeSetting> v) throws IOException {
        return Files.write(Paths.get(filePath), buildRows(v), CREATE);
    }

    private static List<String> buildRows(Set<TemperatureRangeSetting> v) {
        return  v.stream().map(TemperatureRangeSetting::formatForCSVFile).collect(toList());
    }
}

