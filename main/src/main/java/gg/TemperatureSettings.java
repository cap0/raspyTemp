package gg;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Collections.singletonList;

//TODO singleton?
class TemperatureSettings {
    private static final Logger logger = LogManager.getLogger(TemperatureSettings.class);
    private final String tempSettingsFilePath;

    private List<Range<ChronoLocalDateTime<?>>> ranges = new ArrayList<>();
    private List<Double> values = new ArrayList<>();

    TemperatureSettings(String tempSettingsFilePath) {
        this.tempSettingsFilePath = tempSettingsFilePath;
    }

    public void initialize() {
        try (Stream<String[]> peek = Files.lines(Paths.get(tempSettingsFilePath))
                .filter(StringUtils::isNotBlank)
                .map(l -> l.trim().split(";"))
                .peek(r -> ranges.add(Range.between(toDate(r[0]), toDate(r[1]))))
                .peek(r -> values.add(Double.parseDouble(r[2])))) {
            long count = peek.count();
            logger.debug("read " + count + " lines from temperature settings");
        } catch (IOException e) {
            logger.error("Error reading settings file", e);
        }
    }

    boolean set(Double newSettingPoint) {
        if (!isSettingsAllowed(newSettingPoint)) {
            return false;
        }

        try {
            Path source = Paths.get(tempSettingsFilePath);
            Path destination = Paths.get(tempSettingsFilePath + "." + System.currentTimeMillis() + ".bkp");
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
            Files.write(source, singletonList(buildRow(newSettingPoint)), CREATE);
        } catch (InvalidPathException | IOException e) {
            logger.error(e);
            return false;
        }
        return true;
    }

    private boolean isSettingsAllowed(Double newSettingPoint) {
        return newSettingPoint >= 2 && newSettingPoint <= 22;
    }

    //2019-06-11T00:00:00;2019-06-18T10:00:00;17
    private String buildRow(Double newSettingPoint) {
        String startDate = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(ISO_LOCAL_DATE_TIME);
        String endDate = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.MINUTES).format(ISO_LOCAL_DATE_TIME);
        return startDate+";"+endDate+";"+newSettingPoint;
    }

    private LocalDateTime toDate(String text) {
        return LocalDateTime.parse(text, ISO_LOCAL_DATE_TIME);
    }

    double getTemperatureSettingsValueForDate(LocalDateTime actualDateTime) {
        for (int i = 0; i < ranges.size(); i++) {
            if (ranges.get(i).contains(actualDateTime)) {
                return values.get(i);
            }
        }
        logger.warn("temperature settings not found for " + actualDateTime + " using default: 18");
       return 18D;
    }


}
