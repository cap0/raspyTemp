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

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(host, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            File localFile = new File(htmlPageFileToUpload);
            String remoteFilePath = "public_html/"+htmlPageName+".html";

            try(InputStream fileToUpload = new FileInputStream(localFile)) {
                upload(ftpClient, remoteFilePath, fileToUpload);
            }
        } catch (Exception ex) {
            logger.error("Error: " + ex.getMessage(), ex);
        } finally {
            close(ftpClient);
        }
    }

    private void upload(FTPClient ftpClient, String remoteFilePath, InputStream inputStream) throws IOException {
        logger.info("Start uploading "+htmlPageName+" html page in " + remoteFilePath);
        boolean done = ftpClient.storeFile(remoteFilePath, inputStream);
        if (done) {
            logger.info("Html page "+htmlPageName+" has been uploaded successfully.");
        } else{
            logger.warn("Html page "+htmlPageName+ " not uploaded");
        }
    }

    private static void close(FTPClient ftpClient) {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException ex) {
            logger.error("Error: " + ex.getMessage(), ex);
        }
    }
}
