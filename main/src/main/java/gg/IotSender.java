package gg;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

import static gg.Constants.*;
import static gg.Constants.SENSORS_FOLDER;

public class IotSender implements Runnable{
    private static final Logger logger = LogManager.getLogger(Orchestrator.class);
    private final ReadTemperature temperatureReader;
    private final String roomSensorName;
    private final String wortSensorName;
    private final String writeKey;


    public IotSender(Properties p) {
        roomSensorName = p.getProperty(ROOM_SENSOR);
        wortSensorName = p.getProperty(WORT_SENSOR);
        temperatureReader = new ReadTemperature(p.getProperty(SENSORS_FOLDER));
        this.writeKey= p.getProperty(WRITE_KEY);
    }

    public void send() {
        String roomTemperatureValue = temperatureReader.readTemperatureForSensor(roomSensorName);
        String wortTemperatureValue = temperatureReader.readTemperatureForSensor(wortSensorName);

        String uri = "https://api.thingspeak.com/update?api_key=" + writeKey +
                "&field1=" + roomTemperatureValue +
                "&field2=" + wortTemperatureValue;

        HttpGet get = new HttpGet(uri);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpResponse execute = client.execute(get);
            StatusLine statusLine = execute.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                logger.error("Error firing data: " + statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void run() {
        try {
            send();
        } catch (Exception e) {
            logger.error(e);
        }
    }
}
