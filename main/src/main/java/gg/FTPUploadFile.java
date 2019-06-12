package gg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

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
    private final String dataFileToUpload;
    private final String remoteFileName;
    private final ReentrantLock lock;

    FTPUploadFile(Properties p, ReentrantLock lock){
        host = p.getProperty(FTP_HOST);
        port = Integer.parseInt(p.getProperty(FTP_PORT));
        user = p.getProperty(FTP_USER);
        pass = p.getProperty(FTP_PASS);
        dataFileToUpload = p.getProperty(TEMPERATURE_PROCESSED_OUTPUT_FILE);
        remoteFileName = p.getProperty(HTML_PAGE_NAME);
        this.lock = lock;
    }

    public static void main(String[] args) {
        new FTPUploadFile(Util.getProperties(args[0]), new ReentrantLock()).uploadFile();
    }

    @Override
    public void run() {
        try {
            waitForDataProcessCompletion();
            uploadFile();
        } catch (Exception t) {
            logger.fatal(t);
        }
    }

    private void waitForDataProcessCompletion() throws InterruptedException {
        int i = 1;
        while (lock.isLocked() && i <= 5) {
            int sleep = 5000;
            logger.warn("lock is taken. wait for " + sleep + " ms. Attempt " + i + "/5");
            i++;
            Thread.sleep(sleep);
        }

        if (lock.isLocked()) {
            logger.warn("lock is still taken. go anyway");
        }else{
            logger.info("lock released.");
        }
    }

    private void uploadFile() {
        logger.info("Starting FTP Upload");

        FTPClient ftp = new FTPClient();
        int timeout = 60 * 1000;

        ftp.setConnectTimeout(timeout);
        ftp.setDataTimeout(timeout);
        ftp.setDefaultTimeout(timeout);
        try {
            connect(ftp);
            ftp.setSoTimeout(timeout);
            logger.info("connected");
            login(ftp);
            logger.info("login");
            setOptions(ftp);
            String remoteTempFilePath = remoteFileName + "_tmp";
            File localFile = new File(dataFileToUpload);

            try (InputStream fileToUpload = new FileInputStream(localFile)) {
                logger.info("Start uploading " + remoteFileName + " in ftp://" + host+"/"+remoteTempFilePath);
                boolean done = ftp.storeFile(remoteTempFilePath, fileToUpload);
                if (done) {
                    logger.info("File " + remoteFileName + " has been uploaded successfully.");
                    rename(ftp, remoteTempFilePath);
                } else {
                    logger.warn("File " + remoteFileName + " not uploaded");
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
            ftpClient.rename(remoteTempFilePath, remoteFileName +".txt");
        } catch (IOException e) {
            logger.error("Error on rename", e);
            throw e;
        }
    }

    private void login(FTPClient ftpClient) throws IOException {
        try {
            ftpClient.login(user, pass);
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
