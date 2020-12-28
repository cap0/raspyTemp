package gg.TemperatureSetting;

import gg.Orchestrator;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static gg.MyHttpServer.API_GET_SETTINGS;
import static gg.MyHttpServer.API_GET_TEMPERATURE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class HttpServerIntegrationTest {

    @Test
    public void api_temperature() {
        String[] parameters = {
                "/Users/gabriele.gattari/raspyTemp/main/src/main/brewDays/test/ftp.properties",
                "/Users/gabriele.gattari/raspyTemp/main/src/main/brewDays/test/test.properties"
        };
        Orchestrator o = new Orchestrator();
        o.init(parameters);

        given().auth().basic("test", "test").
                when().
                get("http://localhost:8000" + API_GET_TEMPERATURE).
                then().
                log().body().
                statusCode(200).
                contentType(ContentType.JSON).
                body("wort", is("2.2")).
                body("room", is("3.3")
                );
    }

    @Test
    public void api_get() {
        String[] parameters = {
                "/Users/gabriele.gattari/raspyTemp/main/src/main/brewDays/test/ftp.properties",
                "/Users/gabriele.gattari/raspyTemp/main/src/main/brewDays/test/test.properties"
        };
        Orchestrator o = new Orchestrator();
        o.init(parameters);

        Response response = given().auth().basic("test", "test").
                when().
                get("http://localhost:8000" + API_GET_SETTINGS).
                then().
                log().body().
                statusCode(200).
                contentType(ContentType.JSON).
                extract().
                response();

        JsonPath jp = response.jsonPath();
        List<Map<String, String>> list = jp.getList(".");
        assertEquals("2019-03-01T20:00:00", list.get(0).get("from"));
        assertEquals("2019-03-06T20:00:00", list.get(0).get("to"));
        assertEquals(new Float(17.0), list.get(0).get("value"));

        assertEquals("2019-03-06T20:00:00", list.get(1).get("from"));
        assertEquals("2019-03-08T20:00:00", list.get(1).get("to"));
        assertEquals(new Float(18.0), list.get(1).get("value"));
    }
}
