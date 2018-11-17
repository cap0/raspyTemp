package gg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static gg.Constants.*;

public class FTPUploadFile implements Runnable{

    private static final Logger logger = LogManager.getLogger(FTPUploadFile.class);

    private final String host;
    private final int port;
    private final String user;
    private final String pass;
    private final String htmlPageFileToUpload;
    private final String htmlPageName;

    FTPUploadFile(Properties p){
        host = p.getProperty(FTP_HOST);
        port = Integer.parseInt(p.getProperty(FTP_PORT));
        user = p.getProperty(FTP_USER);
        pass = p.getProperty(FTP_PASS);
        htmlPageFileToUpload = p.getProperty(HTML_OUTPUT_FILE);
        htmlPageName = p.getProperty(HTML_PAGE_NAME);
    }

    public static void main(String[] args) {
        new FTPUploadFile(Util.getProperties(args[0])).uploadFile();
    }

    @Override
    public void run() {
        try {
            uploadFile();
        } catch (Throwable t) {
            logger.fatal(t);
        }
    }

    private void uploadFile() {
        logger.info("Starting FTP Upload");

        FTPClient ftp = new FTPClient();
        try {
            connect(ftp);
            login(ftp);
            setOptions(ftp);
            String remoteTempFilePath = htmlPageName + "_tmp" + ".html";
            File localFile = new File(htmlPageFileToUpload);

            try (InputStream fileToUpload = new FileInputStream(localFile)) {
                logger.info("Start uploading " + htmlPageName + " html page in " + remoteTempFilePath);
                boolean done = ftp.storeFile(remoteTempFilePath, fileToUpload);
                if (done) {
                    logger.info("Html page " + htmlPageName + " has been uploaded successfully.");
                    rename(ftp, remoteTempFilePath);
                } else {
                    logger.warn("Html page " + htmlPageName + " not uploaded");
                }
            }
        } catch (IOException e) {
            logger.error("Error on upload", e);
        } finally {
            close(ftp);
        }
    }

    private void setOptions(FTPClient ftpClient) throws IOException {
        try {
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        } catch (IOException e) {
            logger.error("Error on set options", e);
            throw e;
        }
    }

    private void rename(FTPClient ftpClient, String remoteTempFilePath) throws IOException {
        try {
            ftpClient.rename(remoteTempFilePath, htmlPageName+".html");
        } catch (IOException e) {
            logger.error("Error on rename", e);
            throw e;
        }
    }

    private boolean login(FTPClient ftpClient) throws IOException {
        try {
            return ftpClient.login(user, pass);
        } catch (IOException e) {
            logger.error("Error on login", e);
            throw e;
        }
    }

    private void connect(FTPClient ftpClient) throws IOException {
        try {
            ftpClient.connect(host, port);
        } catch (IOException e) {
            logger.error("Error on connect", e);
            throw e;
        }
    }

    private static void close(FTPClient ftpClient) {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException ex) {
            logger.error("Error on close: " + ex.getMessage(), ex);
        }
    }
}
