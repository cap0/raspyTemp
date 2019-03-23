package gg;

import org.apache.commons.lang3.Range;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.LinkedHashMap;

public class ControllerTest {
    Mockery context = new Mockery();
    private String wortSensorName;
    private TemperatureSettings temperatureSettings;

    @Before
    public void init(){
     /*   wortSensorName = "testSensorName";
        temperatureSettings = new TemperatureSettings()
        LocalDateTime d1 = LocalDateTime.of(2019, 1, 1, 0, 0);
        LocalDateTime d2 = LocalDateTime.of(2019, 1, 5, 0, 0);
        LocalDateTime d3 = LocalDateTime.of(2019, 1, 7, 0, 0);

        Range<ChronoLocalDateTime<?>> r1 = Range.between(d1, d2);
        Range<ChronoLocalDateTime<?>> r2 = Range.between(d2, d3);
        temperatureSettings.put(r1,  18D);
        temperatureSettings.put(r2,  19D);
        */
    }
/*
    @Test
    public void startBeltIfTemperatureIsLower() {
        IReadTemperature temperatureReader = context.mock(IReadTemperature.class);

        IGPIOController gpioCtrl = context.mock(IGPIOController.class);
        Controller controller = new Controller(temperatureReader, wortSensorName, temperatureSettings, 0.5D, gpioCtrl);

        context.checking(new Expectations() {{
            oneOf(controller.temperatureReader).readTemperatureForSensor(wortSensorName);
            will(returnValue("17"));

            oneOf(controller.gpioCtrl).startBelt();
        }});
        LocalDateTime now = LocalDateTime.of(2019, 1, 1, 1, 0);
        controller.checkIfTempIsOnRange(now);
    }

    @Test
    public void stopAllIfTemperatureIsOK() {
        IReadTemperature temperatureReader = context.mock(IReadTemperature.class);

        IGPIOController gpioCtrl = context.mock(IGPIOController.class);
        Controller controller = new Controller(temperatureReader, wortSensorName, temperatureSettings, 0.5D, gpioCtrl);

        context.checking(new Expectations() {{
            oneOf(controller.temperatureReader).readTemperatureForSensor(wortSensorName);
            will(returnValue("17.6"));

            oneOf(controller.gpioCtrl).stop();
        }});
        LocalDateTime now = LocalDateTime.of(2019, 1, 1, 1, 0);
        controller.checkIfTempIsOnRange(now);
    }

    @Test
    public void startFridgeIfTemperatureIsUpper() {
        IReadTemperature temperatureReader = context.mock(IReadTemperature.class);

        IGPIOController gpioCtrl = context.mock(IGPIOController.class);
        Controller controller = new Controller(temperatureReader, wortSensorName, temperatureSettings, 0.5D, gpioCtrl);

        context.checking(new Expectations() {{
            oneOf(controller.temperatureReader).readTemperatureForSensor(wortSensorName);
            will(returnValue("18.6"));

            oneOf(controller.gpioCtrl).startFridge();
        }});
        LocalDateTime now = LocalDateTime.of(2019, 1, 1, 1, 0);
        controller.checkIfTempIsOnRange(now);
    }
    */
}