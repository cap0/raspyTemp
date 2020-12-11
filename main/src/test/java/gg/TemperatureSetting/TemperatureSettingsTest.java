package gg.TemperatureSetting;

import org.apache.commons.lang3.Range;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
        assertFalse(temperatureSettings.setTemperaturePoint(null, LocalDateTime.now()));
    }

    @Test
    public void setAcceptedWhenEmpty() throws IOException {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        context.checking(new Expectations() {{
            Set<TemperatureRangeSetting> setValues = new TreeSet<>();
            TemperatureRangeSetting trs = new TemperatureRangeSetting(Range.between(now, now.plusDays(30)), 3D);
            setValues.add(trs);
            oneOf(sourceHandler).backupAndWriteFile(with(setValues));
        }});

        assertTrue(temperatureSettings.setTemperaturePoint(3D, now));
    }

    @Test
    public void setAccepted() throws IOException {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        TreeSet<TemperatureRangeSetting> testValues = new TreeSet<>();
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

        assertTrue(temperatureSettings.setTemperaturePoint(15D, settingsDate));
    }

    @Test
    public void getTemperatureSettingsValueForDate() {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        TreeSet<TemperatureRangeSetting> testValues = new TreeSet<>();
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

    @Test
    public void rampDownIsGenerated() {

        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        LocalDateTime startDate = LocalDateTime.of(2020, Month.MARCH, 1, 14, 15, 14);
        Set<TemperatureRangeSetting> ramp = temperatureSettings.generateRampDown(20, 2, startDate);

        assertEquals(18, ramp.size());

        List<TemperatureRangeSetting> l = new ArrayList<>(ramp);
        TemperatureRangeSetting first = l.get(0);
        assertEquals(19D, first.getValue(), 0.0);
        LocalDateTime startOfFirstInterval = LocalDateTime.of(2020, Month.MARCH, 1, 14, 15, 0);
        assertEquals(startOfFirstInterval, first.getMinimum());
        assertEquals(LocalDateTime.of(2020, Month.MARCH, 1, 17, 15, 0), first.getMaximum());


        TemperatureRangeSetting last = l.get(17);
        assertEquals(2D, last.getValue(), 0.0);
        assertEquals(startOfFirstInterval.plusHours(51L), last.getMinimum());
        assertEquals(startOfFirstInterval.plusHours(54L), last.getMaximum());
    }

    @Test
    public void mergeSetPointWithSettingsWhenEmpty() {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        LocalDateTime date = LocalDateTime.of(2020, Month.MARCH, 1, 17, 15, 0);
        Set<TemperatureRangeSetting> temperatureRangeSettings = temperatureSettings.reduceSettingsToDate(date, new TreeSet<>());

        assertEquals(0, temperatureRangeSettings.size());
    }

    @Test
    public void mergeSetPointWithSettings() {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);
        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        LocalDateTime date = LocalDateTime.of(2000, 1, 6, 22, 0, 0);

        TreeSet<TemperatureRangeSetting> testValues = new TreeSet<>();
        TemperatureRangeSetting trs0 = new TemperatureRangeSetting(Range.between(
                LocalDateTime.of(2000, 1, 1, 13, 10),
                LocalDateTime.of(2000, 1, 5, 17, 10)), 5D);
        testValues.add(trs0);
        TemperatureRangeSetting trs1 = new TemperatureRangeSetting(Range.between(
                LocalDateTime.of(2000, 1, 5, 17, 10),
                LocalDateTime.of(2000, 1, 10, 20, 10)), 10D);
        testValues.add(trs1);

        Set<TemperatureRangeSetting> temperatureRangeSettings = temperatureSettings.reduceSettingsToDate(date, testValues);

        assertEquals(2, temperatureRangeSettings.size());

        Iterator<TemperatureRangeSetting> iter = temperatureRangeSettings.iterator();
        TemperatureRangeSetting first = iter.next();

        assertEquals(trs0.getValue(), first.getValue(), 0.0);
        assertEquals(trs0.getMinimum(), first.getMinimum());
        assertEquals(trs0.getMaximum(), first.getMaximum());

        TemperatureRangeSetting second = iter.next();

        assertEquals(trs1.getValue(), second.getValue(), 0.0);
        assertEquals(trs1.getMinimum(), second.getMinimum());
        assertEquals(date, second.getMaximum());

    }

    @Test
    public void applyRampDown() throws IOException {
        ITemperatureSettingsSourceHandler sourceHandler = context.mock(ITemperatureSettingsSourceHandler.class);

        TemperatureSettings temperatureSettings = new TemperatureSettings(sourceHandler);

        TreeSet<TemperatureRangeSetting> testValues = new TreeSet<>();
        TemperatureRangeSetting trs0 = new TemperatureRangeSetting(Range.between(
                LocalDateTime.of(2000, 1, 1, 13, 10),
                LocalDateTime.of(2000, 1, 5, 17, 10)), 5D);
        testValues.add(trs0);
        LocalDateTime startSecondRange = LocalDateTime.of(2000, 1, 5, 17, 10);
        TemperatureRangeSetting trs1 = new TemperatureRangeSetting(Range.between(
                startSecondRange,
                LocalDateTime.of(2000, 1, 10, 20, 10)), 10D);
        testValues.add(trs1);

        temperatureSettings.settings = testValues;

        LocalDateTime date = LocalDateTime.of(2000, 1, 7, 17, 15, 0);

        context.checking(new Expectations() {{
            oneOf(sourceHandler).backupAndWriteFile(with(any(Set.class)));
        }});

        temperatureSettings.applyRampDown(2D, date); //XXX

        assertEquals(11, temperatureSettings.settings.size());
        Iterator<TemperatureRangeSetting> it = temperatureSettings.settings.iterator();
        assertEquals(trs0, it.next());
        assertEquals(new TemperatureRangeSetting(Range.between(startSecondRange, date), null), it.next());

        assertEquals(new TemperatureRangeSetting(Range.between(date, date.plusHours(3)), null), it.next());
        assertEquals(new TemperatureRangeSetting(Range.between(date.plusHours(3), date.plusHours(6)), null), it.next());
        assertEquals(new TemperatureRangeSetting(Range.between(date.plusHours(6), date.plusHours(9)), null), it.next());
        assertEquals(new TemperatureRangeSetting(Range.between(date.plusHours(9), date.plusHours(12)), null), it.next());
        assertEquals(new TemperatureRangeSetting(Range.between(date.plusHours(12), date.plusHours(15)), null), it.next());
        assertEquals(new TemperatureRangeSetting(Range.between(date.plusHours(15), date.plusHours(18)), null), it.next());
        assertEquals(new TemperatureRangeSetting(Range.between(date.plusHours(18), date.plusHours(21)), null), it.next());
        assertEquals(new TemperatureRangeSetting(Range.between(date.plusHours(21), date.plusHours(24)), null), it.next());

        assertEquals(new TemperatureRangeSetting(Range.between(date.plusHours(24), date.plusHours(24).plusMonths(1)), 2D), it.next());

        List<Double> values = temperatureSettings.settings.stream().map(TemperatureRangeSetting::getValue).collect(Collectors.toList());
        assertEquals(Arrays.asList(5.0, 10.0, 9.0, 8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0,2.0), values);
    }

}