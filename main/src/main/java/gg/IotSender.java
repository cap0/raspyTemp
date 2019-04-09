package gg;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static gg.Constants.*;

public class IotSender implements Runnable{
    private static final Logger logger = LogManager.getLogger(Orchestrator.class);
    private final TemperatureReader temperatureReader;
    private final String writeKey;


    IotSender(Properties p) {
        temperatureReader = new TemperatureReader(p.getProperty(SENSORS_FOLDER), p.getProperty(WORT_SENSOR), p.getProperty(ROOM_SENSOR));
        writeKey= p.getProperty(IOT_WRITE_KEY);
    }

    private void send() throws URISyntaxException {
        String roomTemperatureValue = temperatureReader.getRoomTemperature();
        String wortTemperatureValue = temperatureReader.getWorthTemperature();

        HttpUriRequest httpGet = buildURL(roomTemperatureValue, wortTemperatureValue);
        HttpClient httpClient = getHttpClient();

        try {
            logger.debug("Sending data " + httpGet);
            HttpResponse execute = httpClient.execute(httpGet);
            StatusLine statusLine = execute.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                logger.error("Error firing data: " + statusLine.getReasonPhrase());
            }
            logger.debug("send");
        } catch (IOException e) {
            logger.error("Error firing data to IOT endpoint", e);
        }
    }

    private HttpUriRequest buildURL(String roomTemperatureValue, String wortTemperatureValue) throws URISyntaxException {
        URI url = getUri(roomTemperatureValue, wortTemperatureValue);
        return new HttpGet(url);
    }

    private HttpClient getHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30 * 1000).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
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
