package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ConnectionChecker implements Runnable {
    private static final Logger logger = LogManager.getLogger(ConnectionChecker.class);

    private Boolean connectionAvailable = false;
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

    public static void main(String[] a){
        logger.info("Schedule Connection checker");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ConnectionChecker task = new ConnectionChecker();

        logger.info("Connection Checker Process. initialDelay= " + 0 + " periodicDelay= " + 1 + " period= " + SECONDS);
        scheduler.scheduleAtFixedRate(task, 0, 1, SECONDS);
    }
}
