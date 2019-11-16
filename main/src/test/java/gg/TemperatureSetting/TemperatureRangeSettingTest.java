package gg.TemperatureSetting;

import org.apache.commons.lang3.Range;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class TemperatureRangeSettingTest {

    @Test
    public void formatForJSONFile() {
        LocalDateTime start = LocalDateTime.of(2000, 1, 5, 17, 10);
        LocalDateTime end = LocalDateTime.of(2000, 1, 10, 20, 10);
        TemperatureRangeSetting trs1 = new TemperatureRangeSetting(Range.between(
                start,
                end), 10D);

        TemperatureRangeSetting.JsonValue jsonValue = trs1.formatForJSONFile();
        assertEquals(10d, jsonValue.value);
    }
}