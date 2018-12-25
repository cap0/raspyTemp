package gg;


import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import javax.mail.MessagingException;
import java.util.Properties;

import static gg.Constants.ROOM_SENSOR;
import static gg.Constants.WORT_SENSOR;


public class TemperatureAlarmTest {
    Mockery context = new Mockery();


    @Test
    public void validTemperatureDoNotFireAlarm() throws MessagingException {
        Properties p = new Properties();
         p.setProperty(ROOM_SENSOR, "roomSensorName");
        p.setProperty(WORT_SENSOR, "wortSensorName");
        TemperatureAlarm temperatureAlarm = new TemperatureAlarm(p);

        temperatureAlarm.mailSender = context.mock(IGMailSender.class);
        temperatureAlarm.temperatureReader = context.mock(IReadTemperature.class);

        context.checking(new Expectations() {{
            oneOf(temperatureAlarm.temperatureReader).readTemperatureForSensor("wortSensorName");
                will(returnValue("18"));

            oneOf(temperatureAlarm.temperatureReader).readTemperatureForSensor("roomSensorName");
                will(returnValue("19"));

            never(temperatureAlarm.mailSender).sendAlarm(with(any(String.class)), with(any(String.class)));

        }});

        temperatureAlarm.run();

        context.assertIsSatisfied();
    }


    @Test
    public void lowerTemperatureFireAlarm() throws MessagingException {
        Properties p = new Properties();
        p.setProperty(ROOM_SENSOR, "roomSensorName");
        p.setProperty(WORT_SENSOR, "wortSensorName");
        TemperatureAlarm temperatureAlarm = new TemperatureAlarm(p);

        temperatureAlarm.mailSender = context.mock(IGMailSender.class);
        temperatureAlarm.temperatureReader = context.mock(IReadTemperature.class);

        context.checking(new Expectations() {{
            allowing(temperatureAlarm.temperatureReader).readTemperatureForSensor("roomSensorName");

            oneOf(temperatureAlarm.temperatureReader).readTemperatureForSensor("wortSensorName");
            will(returnValue("-1"));

            oneOf(temperatureAlarm.temperatureReader).readTemperatureForSensor("wortSensorName");
            will(returnValue("-2"));

            oneOf(temperatureAlarm.temperatureReader).readTemperatureForSensor("wortSensorName");
            will(returnValue("-3"));

            never(temperatureAlarm.mailSender).sendAlarm(with(any(String.class)), with(any(String.class)));

        }});

        temperatureAlarm.run();

   //     context.assertIsSatisfied();
    }
}
