package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;

public class ConnectionChecker implements Runnable {
    private static final Logger logger = LogManager.getLogger(ConnectionChecker.class);

    private volatile Boolean connectionAvailable = false;
    private LocalDateTime lastActive = null;
    private boolean logPrinted = false;

    @Override
    public void run() {
        try {
            final URL url = new URL("http://www.google.com");
            final URLConnection conn = url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.connect();
            conn.getInputStream().close();
            connectionAvailable = true;
            lastActive = LocalDateTime.now();
            if (logPrinted) {
                logger.warn("connection restored " + lastActive);
            }
            logPrinted = false;
        } catch (Exception e) {
            connectionAvailable = false;
            if (!logPrinted) {
                logger.warn("no connection available, last time connected: " + (lastActive!=null?lastActive:"never"));
            }
            logPrinted = true;
        }
    }

    boolean isConnectionAvailable(){
        return connectionAvailable;
    }
}
