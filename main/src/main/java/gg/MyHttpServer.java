package gg;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;

public class MyHttpServer {

    private static final String API = "/api/set/";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext(API, (exchange -> {
            URI requestURI = exchange.getRequestURI();
            String param = requestURI.toString().substring(API.length());

            Optional<Double> aDouble = isDouble(param);

            String respText;
            if (aDouble.isPresent()) {
                ad
                respText = "set:" + aDouble.get();
                exchange.sendResponseHeaders(200, respText.getBytes().length);
            } else {
                respText = "invalid:" + param;
                exchange.sendResponseHeaders(500, respText.getBytes().length);
            }
            writeResponse(exchange, respText);
        }));
        server.setExecutor(null); // creates a default executor
        server.start();
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
