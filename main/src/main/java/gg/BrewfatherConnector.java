package gg;

import com.google.gson.Gson;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static gg.util.PropertyUtil.*;
import static gg.util.Util.getHttpClient;

public class BrewfatherConnector implements Runnable{
    private final String brewfatherId;
    private final TemperatureReader temperatureReader;
    private final String brefatherURL;
    private static final Logger logger = LogManager.getLogger(BrewfatherConnector.class);

    public BrewfatherConnector(Properties p){
        temperatureReader = new TemperatureReader(p.getProperty(SENSORS_FOLDER), p.getProperty(WORT_SENSOR), p.getProperty(ROOM_SENSOR));
        this.brewfatherId = p.getProperty(BREWFATHER_ID);
        this.brefatherURL =  p.getProperty(BREWFATHER_URL);
    }

    private void send() throws IOException {
        HttpClient httpClient = getHttpClient();
        HttpPost post = new HttpPost(brefatherURL);
        post.setHeader("Content-type", "application/json");
        String bodyJson = buildBody(temperatureReader.getWortTemperature(), temperatureReader.getRoomTemperature());

        StringEntity stringEntity = new StringEntity(bodyJson);
        post.setEntity(stringEntity);
        httpClient.execute(post);

        logger.info("Json sent " + bodyJson);
    }

    /**
     * //JSON Example see https://docs.brewfather.app/integrations/custom-stream
     * {
     * "name": "YourDeviceName", // Required field, this will be the ID in Brewfather
     * "temp": 20.32,
     * "aux_temp": 15.61, // Fridge Temp
     * "ext_temp": 6.51, // Room Temp
     * "temp_unit": "C", // C, F, K
     * "gravity": 1.042,
     * "gravity_unit": "G", // G, P
     * "pressure": 10,
     * "pressure_unit": "PSI", // PSI, BAR, KPA
     * "ph": 4.12,
     * "bpm": 123, // Bubbles Per Minute
     * "comment": "Hello World",
     * "beer": "Pale Ale"
     * }
     */

    private String buildBody(String wortTemp, String roomTemp) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", brewfatherId);
        m.put("temp", Double.valueOf(wortTemp));
        m.put("aux_temp", Double.valueOf(roomTemp));
        m.put("temp_unit", "C");
        return new Gson().toJson(m);
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
