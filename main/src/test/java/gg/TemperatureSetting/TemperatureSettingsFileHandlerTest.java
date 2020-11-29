package gg.TemperatureSetting;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.apache.commons.lang3.Range.between;
import static org.junit.Assert.assertEquals;

public class TemperatureSettingsFileHandlerTest {

    @Test
    public void rampDown() throws IOException {
        String path = "temp.set";
        TemperatureSettingsFileHandler t = new TemperatureSettingsFileHandler(path);
        LocalDateTime d = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        TreeSet<TemperatureRangeSetting> v = new TreeSet<>();
        LocalDateTime currentDate = t.decreaseTemperatureToTarget(v, d, 21, 1D);

        v.add(new TemperatureRangeSetting(between(currentDate, currentDate.plusDays(10)), 1D));
        t.write(v);
    }

    @Test
    public void rampUp() throws IOException {
        String path = "tempUp.txt";
        TemperatureSettingsFileHandler t = new TemperatureSettingsFileHandler(path);
        LocalDateTime d = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        TreeSet<TemperatureRangeSetting> v = new TreeSet<>();
        LocalDateTime currentDate = decreaseTemperatureToTarget(v, d, 3, 18);

        t.write(v);
    }

    LocalDateTime decreaseTemperatureToTarget(Set<TemperatureRangeSetting> v, LocalDateTime currentDate, double currentTemp, double targetTemperature) {
        while (currentTemp< targetTemperature) {
            currentTemp += 1;
            LocalDateTime newEnd = currentDate.plusMinutes(90);
            v.add(new TemperatureRangeSetting(between(currentDate, newEnd), currentTemp));
            currentDate = newEnd;
        }
        return currentDate;
    }
}