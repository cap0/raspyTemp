package gg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class FTPUploadFile {


    public static void main(String[] args) {
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

            System.out.println("Start uploading file");
            boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                System.out.println("The first file is uploaded successfully.");
            }

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
