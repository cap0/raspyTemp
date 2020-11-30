package gg;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static gg.util.PropertyUtil.*;
import static gg.util.Util.executeHttpRequest;
import static gg.util.Util.getHttpClient;

public class IotSender implements Runnable{ //TODO bulk upload
    private static final Logger logger = LogManager.getLogger(Orchestrator.class);
    private final TemperatureReader temperatureReader;
    private final String writeKey;

    IotSender(Properties p) {
        temperatureReader = new TemperatureReader(p.getProperty(SENSORS_FOLDER), p.getProperty(WORT_SENSOR), p.getProperty(ROOM_SENSOR));
        writeKey = p.getProperty(IOT_WRITE_KEY);
    }

    private void send() throws URISyntaxException {
        String roomTemperatureValue = temperatureReader.getRoomTemperature();
        String wortTemperatureValue = temperatureReader.getWortTemperature();

        executeHttpRequest(buildURL(roomTemperatureValue, wortTemperatureValue), getHttpClient());
        logger.info("upload completed on IOT: Room: "+ roomTemperatureValue + " wort" + wortTemperatureValue);
    }

    private HttpUriRequest buildURL(String roomTemperatureValue, String wortTemperatureValue) throws URISyntaxException {
        return new HttpGet(getUri(roomTemperatureValue, wortTemperatureValue));
    }

    private URI getUri(String roomTemperatureValue, String wortTemperatureValue) throws URISyntaxException {
        URIBuilder b = new URIBuilder("https://api.thingspeak.com/update");
        b.setParameter("api_key", writeKey);
        b.setParameter("field1", roomTemperatureValue);
        b.setParameter("field2", wortTemperatureValue);
        return b.build();
    }

    @Override
    public void run() {
        try {
            send();
        } catch (Exception e) {
            logger.error("Error during IOT sending", e);
        }
    }
}
