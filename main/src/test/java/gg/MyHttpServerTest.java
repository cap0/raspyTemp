package gg;

import gg.notify.INotifier;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyHttpServerTest {

    Mockery context = new Mockery();

    @Test
    public void startHttpServer() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        INotifier notif = context.mock(INotifier.class);
        IReadTemperature reader = context.mock(IReadTemperature.class);

        MyHttpServer myHttpServer = new MyHttpServer("user", "pass", notif, reader, null);
        myHttpServer.startHttpServer();

        context.checking(new Expectations() {{
            oneOf(myHttpServer.temperatureReader).getTemperatureRaw();
            will(returnValue(new TemperatureRaw("23", "45")));
        }});

        URL url = new URL("http://localhost:8000/api/temperature/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        String encoded = Base64.getEncoder().encodeToString(("user"+":"+"pass").getBytes(StandardCharsets.UTF_8));  //Java 8
        con.setRequestProperty("Authorization", "Basic "+encoded);
        con.setRequestMethod("GET");

        int status = con.getResponseCode();

        assertEquals(200, status);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        assertTrue(content.toString().contains("23"));
        assertTrue(content.toString().contains("45"));
    }
}