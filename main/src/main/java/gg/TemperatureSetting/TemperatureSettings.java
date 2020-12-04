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

    /**
     * Create a ramp of steps of decreasing temperature ( 1 degree each 3 hour )
     * @param targetTemp temperature to reach
     * @param setPointDate date to start the ramp
     */
    public void applyRampDown(double targetTemp, LocalDateTime setPointDate) {
        settings = reduceSettingsToDate(setPointDate, settings);
        Set<TemperatureRangeSetting> ramp = generateRampDown(getTemperatureSettingsValueForDate(setPointDate), targetTemp, setPointDate);
        settings.addAll(ramp);
    }

    public Set<TemperatureRangeSetting> reduceSettingsToDate(LocalDateTime setPointDate, Set<TemperatureRangeSetting> alreadyPresentSettings) {
        Set<TemperatureRangeSetting> newRange = new TreeSet<>();
        for (TemperatureRangeSetting t : alreadyPresentSettings) {
            if (t.isBefore(setPointDate)) {
                newRange.add(t);
            } else if (t.contains(setPointDate)) {
                newRange.add(new TemperatureRangeSetting(Range.between(t.getMinimum(), setPointDate), t.getValue()));
            }
        }

        return newRange;
    }

    public Set<TemperatureRangeSetting> generateRampDown(double currentTemp, double targetTemperature, LocalDateTime startDate) {
        Set<TemperatureRangeSetting> v = new TreeSet<>();
        LocalDateTime currentDate = startDate.truncatedTo(ChronoUnit.MINUTES);
        while (currentTemp > targetTemperature) {
            currentTemp -= 1;
            LocalDateTime newEnd = currentDate.plusHours(3);
            v.add(new TemperatureRangeSetting(between(currentDate, newEnd), currentTemp));
            currentDate = newEnd;
        }

        return v;
    }

    private Range<ChronoLocalDateTime<?>> getNewRange(LocalDateTime now) {
        LocalDateTime endDate = now.plusDays(30);
        return Range.between(now, endDate);
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
