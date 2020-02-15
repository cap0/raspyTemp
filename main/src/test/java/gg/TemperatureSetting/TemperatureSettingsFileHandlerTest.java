package gg.TemperatureSetting;

import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.apache.commons.lang3.Range.between;
import static org.junit.Assert.*;

public class TemperatureSettingsFileHandlerTest {

    @Test
    public void rampDown() throws IOException {
        String path ="temp.set";
        TemperatureSettingsFileHandler t = new TemperatureSettingsFileHandler(path);
        LocalDateTime d = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        TreeSet<TemperatureRangeSetting> v = new TreeSet<>();
        LocalDateTime currentDate = t.rampDown(v, d, 20);

        v.add(new TemperatureRangeSetting(between(currentDate, currentDate.plusDays(10)), 2D));
        t.write(v);
    }
}