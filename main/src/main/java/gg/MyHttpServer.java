package gg;

import com.sun.net.httpserver.*;
import gg.notify.TelegramNotifier;
import gg.TemperatureSetting.TemperatureSettings;
import gg.TemperatureSetting.TemperatureSettingsFileHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static gg.util.PropertyUtil.*;

public class MyHttpServer {
    private static final Logger logger = LogManager.getLogger(MyHttpServer.class);

    private static final String API = "/api/set/";
    private final TemperatureSettings temperatureSettings;
    private String username;
    private String password;
    private TelegramNotifier telegramNotifier;

    public static void main(String[] args) {
        MyHttpServer myHttpServer = new MyHttpServer(args[0], args[1], args[2]);
        myHttpServer.startHttpServer();
    }

    public MyHttpServer(Properties p) {
        String temperatureSettingsPath = getTemperatureSettingsPath(p);
        temperatureSettings = new TemperatureSettings(new TemperatureSettingsFileHandler(temperatureSettingsPath));
        username = getUsername(p);
        password = getPassword(p);
        telegramNotifier = new TelegramNotifier(p);
    }

    private MyHttpServer(String temperatureSettingsPath, String username, String password) {
        temperatureSettings = new TemperatureSettings(new TemperatureSettingsFileHandler(temperatureSettingsPath));
        this.username = username;
        this.password = password;
    }

    public void startHttpServer() {
        logger.info("Starting Http Server");
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(8000), 0);
        } catch (IOException e) {
            logger.error("cannot star http server", e);
            return;
        }

        HttpContext context = server.createContext(API, getHttpHandler());
        context.setAuthenticator(getBasicAuthenticator());

        server.setExecutor(null); // creates a default executor
        server.start();
    }

    private HttpHandler getHttpHandler() {
        return exchange -> {
            URI requestURI = exchange.getRequestURI();
            String param = requestURI.toString().substring(API.length());
            String message = "Temperature setting: " + param;
            logger.info(message);
            telegramNotifier.sendNotify(message);

            Optional<Double> aDouble = isDouble(param);
            if (aDouble.isPresent()) {
                temperatureSettings.initialize();
                boolean successful = temperatureSettings.set(aDouble.get(), LocalDateTime.now());
                respond(exchange, aDouble.get().toString(), successful ? "set:" : "rejected:", successful ? 200 : 500);
            } else {
                respond(exchange, param, "invalid:", 500);
            }
        };
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
