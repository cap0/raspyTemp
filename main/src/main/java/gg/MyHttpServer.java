package gg;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;

import static gg.util.PropertyUtil.getTemperatureSettingsPath;

public class MyHttpServer {
    private static final Logger logger = LogManager.getLogger(MyHttpServer.class);

    private static final String API = "/api/set/";
    private final TemperatureSettings temperatureSettings;

    public static void main(String[] args) throws IOException {
        MyHttpServer myHttpServer = new MyHttpServer(args[0]);
        myHttpServer.startHttpServer();
    }

    public MyHttpServer(Properties p) {
        String temperatureSettingsPath = getTemperatureSettingsPath(p);
        temperatureSettings = new TemperatureSettings(temperatureSettingsPath);
    }

    private MyHttpServer(String temperatureSettingsPath) {
        temperatureSettings = new TemperatureSettings(temperatureSettingsPath);
    }

    private void startHttpServer() throws IOException {
        logger.info("Starting Http Server");
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext(API, (exchange -> {
            URI requestURI = exchange.getRequestURI();
            String param = requestURI.toString().substring(API.length());
            logger.info("got request: '{}'", param);

            //TODO add auth
            Optional<Double> aDouble = isDouble(param);
            if (aDouble.isPresent()) {
                boolean successful = temperatureSettings.set(aDouble.get());
                respond(exchange, aDouble.get().toString(), successful ? "set:" : "rejected:", successful ? 200 : 500);
            } else {
                respond(exchange, param, "invalid:", 500);
            }
        }));
        server.setExecutor(null); // creates a default executor
        server.start();
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
