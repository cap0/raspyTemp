package gg;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

class TemperatureSettings {
    private static final Logger logger = LogManager.getLogger(TemperatureSettings.class);

    private List<Range<ChronoLocalDateTime<?>>> ranges = new ArrayList<>();
    private List<Double> values = new ArrayList<>();

    TemperatureSettings(String tempSettingsFilePath) {
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
