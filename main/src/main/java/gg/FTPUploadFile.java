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

public class FTPUploadFile {

    private static final Logger logger = LogManager.getLogger(FTPUploadFile.class);

    public static void main(String[] args) {
        uploadFile(args);
    }

    private static void uploadFile(String[] args) {
        Properties p = Util.getProperties(args);

        FTPClient ftpClient = new FTPClient();
        try {

            ftpClient.connect(p.getProperty("host"), Integer.parseInt(p.getProperty("port")));
            ftpClient.login(p.getProperty("user"), p.getProperty("pass"));
            ftpClient.enterLocalPassiveMode();

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            File firstLocalFile = new File(p.getProperty("file"));

            String firstRemoteFile = "public_html/index.html";
            InputStream inputStream = new FileInputStream(firstLocalFile);

            logger.info("Start uploading file");
            boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                logger.info("File is uploaded successfully.");
            }

        } catch (IOException ex) {
            logger.error("Error: " + ex.getMessage(), ex);
        } finally {
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

}
