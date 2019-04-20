package gg;

import java.net.URL;
import java.net.URLConnection;

public class ConnectionChecker implements Runnable {

    private Boolean connectionAvailable = false;

    @Override
    public void run() {
        try {
            final URL url = new URL("http://www.google.com");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            connectionAvailable = true;
        } catch (Exception e) {
            connectionAvailable = false;
        }
    }

    boolean isConnectionAvailable(){
        return connectionAvailable;
    }
}
