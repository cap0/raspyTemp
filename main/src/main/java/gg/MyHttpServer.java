package gg;

import com.sun.net.httpserver.*;
import gg.TemperatureSetting.TemperatureSettings;
import gg.TemperatureSetting.TemperatureSettingsFileHandler;
import gg.notify.INotifier;
import gg.notify.TelegramNotifier;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static gg.util.PropertyUtil.*;

public class MyHttpServer {
    private static final Logger logger = LogManager.getLogger(MyHttpServer.class);

    private static final String API_SET = "/api/set/";
    private static final String API_RAMP_DOWN = "/api/ramp-down/";
    public static final String API_GET_SETTINGS = "/api/get/";
    public static final String API_GET_TEMPERATURE = "/api/temperature/";

    private final TemperatureSettings temperatureSettings;
    IReadTemperature temperatureReader;
    private final String username;
    private final String password;
    private final INotifier telegramNotifier;

    public static void main(String[] args) {
        MyHttpServer myHttpServer = new MyHttpServer(args[1], args[2], null, null, new TemperatureSettings(new TemperatureSettingsFileHandler(args[0])));
        myHttpServer.startHttpServer();
    }

    MyHttpServer(Properties p) {
        this(getUsername(p), getPassword(p),
                new TelegramNotifier(p),
                new TemperatureReader(p),
                new TemperatureSettings(new TemperatureSettingsFileHandler(getTemperatureSettingsPath(p))));
    }

     MyHttpServer(String username, String password, INotifier notifier, IReadTemperature reader, TemperatureSettings temperatureSettings) {
        this.temperatureSettings = temperatureSettings;
        this.username = username;
        this.password = password;
        this.telegramNotifier = notifier;
        this.temperatureReader = reader;
    }

    void startHttpServer() {
        logger.info("Starting Http Server");
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(8000), 0);
        } catch (IOException e) {
            logger.error("cannot star http server", e);
            return;
        }

        BasicAuthenticator basicAuthenticator = getBasicAuthenticator();

        HttpContext setCxt = server.createContext(API_SET, handlerSetTempPoint());
        setCxt.setAuthenticator(basicAuthenticator);

        HttpContext rampDown = server.createContext(API_RAMP_DOWN, handlerRampDown());
        rampDown.setAuthenticator(basicAuthenticator);

        HttpContext getSettingsCtx = server.createContext(API_GET_SETTINGS, this::executeGet);
        getSettingsCtx.setAuthenticator(basicAuthenticator);

        HttpContext getTemperatureCtx = server.createContext(API_GET_TEMPERATURE, this::executeGetTemp);
        getTemperatureCtx.setAuthenticator(basicAuthenticator);

        server.setExecutor(null); // creates a default executor
        server.start();
        logger.info("started");
    }

    private HttpHandler handlerSetTempPoint() {
        return exchange -> executeSet(exchange, exchange.getRequestURI().toString());
    }

    private HttpHandler handlerRampDown() {
        return exchange -> executeRampDown(exchange, exchange.getRequestURI().toString());
    }

    private void executeSet(HttpExchange exchange, String uri) throws IOException {
        String param = uri.substring(API_SET.length());
        String message = "Temperature setting: " + param;
        logger.info(message);
        telegramNotifier.sendNotify(message);

        Optional<Double> aDouble = isDouble(param);
        if (aDouble.isPresent()) {
            temperatureSettings.initialize();
            boolean successful = temperatureSettings.setTemperaturePoint(aDouble.get(), LocalDateTime.now());
            respond(exchange, aDouble.get().toString(), successful ? "set:" : "rejected:", successful ? 200 : 500);
        } else {
            respond(exchange, param, "invalid:", 500);
        }
    }

    private void executeRampDown(HttpExchange exchange, String uri) throws IOException {
        String param = uri.substring(API_RAMP_DOWN.length());
        String message = "Temperature ramp down: " + param;
        logger.info(message);
        telegramNotifier.sendNotify(message);

        Optional<Double> aDouble = isDouble(param);
        if (aDouble.isPresent()) {
            temperatureSettings.initialize();
            boolean successful = temperatureSettings.applyRampDown(aDouble.get(), LocalDateTime.now());
            respond(exchange, aDouble.get().toString(), successful ? "set:" : "rejected:", successful ? 200 : 500);
        } else {
            respond(exchange, param, "invalid:", 500);
        }
    }

    private void executeGet(HttpExchange e) throws IOException {
        temperatureSettings.initialize();
        String json = temperatureSettings.toJSON();
        e.getResponseHeaders().add("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        e.sendResponseHeaders(200, json.getBytes().length);
        writeResponse(e, json);
    }

    private void executeGetTemp(HttpExchange e) throws IOException {
        TemperatureRaw t = temperatureReader.getTemperatureRaw();
        e.getResponseHeaders().add("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        e.sendResponseHeaders(200, t.toJSON().getBytes().length);
        writeResponse(e, t.toJSON());
    }

    private BasicAuthenticator getBasicAuthenticator() {
        return new BasicAuthenticator("myrealm") {
            @Override
            public boolean checkCredentials(String user, String pwd) {
                return user.equals(username) && pwd.equals(password);
            }
        };
    }

    private void respond(HttpExchange exchange, String setting, String message, int httpStatus) throws IOException {
        String respText = message + setting;
        exchange.sendResponseHeaders(httpStatus, respText.getBytes().length);
        writeResponse(exchange, respText);
    }

    private static void writeResponse(HttpExchange exchange, String respText) throws IOException {
        OutputStream output = exchange.getResponseBody();
        output.write(respText.getBytes());
        output.flush();
        exchange.close();
    }

    private static Optional<Double> isDouble(String param) {
        try {
           return Optional.of(Double.parseDouble(param));
        } catch(NumberFormatException e) {
            return Optional.empty();
        }
    }
}
