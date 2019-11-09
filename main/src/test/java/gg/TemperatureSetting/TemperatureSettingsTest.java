package gg.TemperatureSetting;

import org.apache.commons.lang3.Range;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TemperatureSettingsTest {
    Mockery context = new Mockery();

    @Test
    public void initialize() {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        List<String> l = new ArrayList<>();
        l.add("2019-03-01T20:00:00;2019-03-06T20:00:00;17");
        l.add("2019-03-06T20:00:00;2019-03-08T20:00:00;18");
        l.add("");
        context.checking(new Expectations() {{
            oneOf(sourceHandler).readLineSettings();
            will(returnValue(l));
        }});

        temperatureSettings.initialize();

        Set<TemperatureRangeSetting> settings = temperatureSettings.settings;
        assertEquals(2, settings.size());

        TemperatureRangeSetting[] r = new TemperatureRangeSetting[2];
        settings.toArray(r);

        assertEquals(LocalDateTime.parse("2019-03-01T20:00:00", ISO_LOCAL_DATE_TIME), r[0].getMinimum());
        assertEquals(LocalDateTime.parse("2019-03-06T20:00:00", ISO_LOCAL_DATE_TIME), r[0].getMaximum());
        assertEquals(17D, r[0].getValue(), 0.0);

        assertEquals(LocalDateTime.parse("2019-03-06T20:00:00", ISO_LOCAL_DATE_TIME), r[1].getMinimum());
        assertEquals(LocalDateTime.parse("2019-03-08T20:00:00", ISO_LOCAL_DATE_TIME), r[1].getMaximum());
        assertEquals(18D, r[1].getValue(), 0.0);
    }

    @Test
    public void setNull() {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);
        assertFalse(temperatureSettings.set(null, LocalDateTime.now()));
    }

    @Test
    public void setOutOfRange() {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);
        assertFalse(temperatureSettings.set(50D, LocalDateTime.now()));
    }

    @Test
    public void setAcceptedWhenEmpty() throws IOException {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        context.checking(new Expectations() {{
            Set<TemperatureRangeSetting> setValues= new TreeSet<>();
            TemperatureRangeSetting trs = new TemperatureRangeSetting(Range.between(now, now.plusDays(30)), 3D);
            setValues.add(trs);
            oneOf(sourceHandler).backupAndWriteFile(with(setValues));
        }});

        assertTrue(temperatureSettings.set(3D, now));
    }

    @Test
    public void setAccepted() throws IOException {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        Set<TemperatureRangeSetting> testValues = new TreeSet<>();
        TemperatureRangeSetting trs0 = new TemperatureRangeSetting(Range.between(
                LocalDateTime.of(2000, 1, 1, 13, 10),
                LocalDateTime.of(2000, 1, 5, 17, 10)), 5D);
        testValues.add(trs0);
        TemperatureRangeSetting trs1 = new TemperatureRangeSetting(Range.between(
                LocalDateTime.of(2000, 1, 5, 17, 10),
                LocalDateTime.of(2000, 1, 10, 20, 10)), 10D);
        testValues.add(trs1);

        temperatureSettings.settings = testValues;

        LocalDateTime settingsDate = LocalDateTime.of(2000, 1, 7, 5, 5).truncatedTo(ChronoUnit.MINUTES);

        context.checking(new Expectations() {{
            Set<TemperatureRangeSetting> setValues = new TreeSet<>();
            setValues.add(trs0);

            TemperatureRangeSetting trs1 = new TemperatureRangeSetting(Range.between(
                    LocalDateTime.of(2000, 1, 5, 17, 10),
                    settingsDate), 10D);
            setValues.add(trs1);

            TemperatureRangeSetting trs2 = new TemperatureRangeSetting(Range.between(settingsDate, settingsDate.plusDays(30)), 15D);
            setValues.add(trs2);

            oneOf(sourceHandler).backupAndWriteFile(with(setValues));
        }});

        assertTrue(temperatureSettings.set(15D, settingsDate));
    }

    @Test
    public void getTemperatureSettingsValueForDate(){
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        Set<TemperatureRangeSetting> testValues = new TreeSet<>();
        TemperatureRangeSetting trs0 = new TemperatureRangeSetting(Range.between(
                LocalDateTime.of(2000, 1, 1, 13, 10),
                LocalDateTime.of(2000, 1, 5, 17, 10)), 5D);
        testValues.add(trs0);
        TemperatureRangeSetting trs1 = new TemperatureRangeSetting(Range.between(
                LocalDateTime.of(2000, 1, 5, 17, 10),
                LocalDateTime.of(2000, 1, 10, 20, 10)), 10D);
        testValues.add(trs1);

        temperatureSettings.settings = testValues;

        assertEquals(18D, temperatureSettings.getTemperatureSettingsValueForDate(LocalDateTime.of(1999, 1, 1, 13, 10)), 0.0);
        assertEquals(5D, temperatureSettings.getTemperatureSettingsValueForDate(LocalDateTime.of(2000, 1, 1, 13, 10)), 0.0);
        assertEquals(10D, temperatureSettings.getTemperatureSettingsValueForDate(LocalDateTime.of(2000, 1, 5, 23, 10)), 0.0);
        assertEquals(18D, temperatureSettings.getTemperatureSettingsValueForDate(LocalDateTime.of(2010, 1, 1, 13, 10)), 0.0);

    }

}