package gg.TemperatureSetting;

import com.google.gson.Gson;
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
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.apache.commons.lang3.Range.between;

public class TemperatureSettings {
    private static final Logger logger = LogManager.getLogger(TemperatureSettings.class);

    final ITemperatureSettingsSourceHandler fileHandler;
    Set<TemperatureRangeSetting> settings = new TreeSet<>();

    public TemperatureSettings(ITemperatureSettingsSourceHandler sourceHandler) {
        this.fileHandler = sourceHandler;
    }

    public void initialize() {
        settings = fileHandler.readLineSettings().stream()
                .filter(StringUtils::isNotBlank)
                .map(l -> l.trim().split(";"))
                .map(r -> new TemperatureRangeSetting(Range.between(toDate(r[0]), toDate(r[1])), Double.parseDouble(r[2])))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean setTemperaturePoint(Double newTempSettingPoint, LocalDateTime time) {
        if (!isTemperatureSettingPointAllowed(newTempSettingPoint)) {
            return false;
        }

        LocalDateTime setPointDate = time.truncatedTo(ChronoUnit.MINUTES);

        settings = reduceSettingsToDate(setPointDate, settings);
        settings.add(new TemperatureRangeSetting(getNewRange(setPointDate), newTempSettingPoint));

        try {
            fileHandler.backupAndWriteFile(settings);
        } catch (InvalidPathException | IOException e) {
            logger.error(e);
            return false;
        }
        return true;
    }

    private Range<ChronoLocalDateTime<?>> getNewRange(LocalDateTime now) {
        LocalDateTime endDate = now.plusDays(30);
        return Range.between(now, endDate);
    }

    private boolean isTemperatureSettingPointAllowed(Double newSettingPoint) {
        return newSettingPoint != null && newSettingPoint >= 2 && newSettingPoint <= 22;
    }

    private LocalDateTime toDate(String text) {
        return LocalDateTime.parse(text, ISO_LOCAL_DATE_TIME);
    }

    public double getTemperatureSettingsValueForDate(LocalDateTime actualDateTime) {
        Optional<TemperatureRangeSetting> found = settings.stream()
                .filter(r -> r.contains(actualDateTime)).
                        findFirst();

        if (found.isPresent()) {
            return found.get().getValue();
        }

        logger.debug("temperature settings not found for " + actualDateTime + " using default: 18");
       return 18D;
    }

    public String toJSON(){
        List<TemperatureRangeSetting.JsonValue> l = new ArrayList<>();
        settings.forEach(s -> l.add(s.formatForJSONFile()));
        Gson gson = new Gson();
        return  gson.toJson(l);
    }

}
