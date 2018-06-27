package gg;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleDriveHelper implements Runnable{
    private static final String APPLICATION_NAME = "raspyTemp";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FOLDER = "credentials"; // Directory to store user credentials.

    private static final Logger logger = LogManager.getLogger(GoogleDriveHelper.class);

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_APPDATA);
    private static final String CLIENT_SECRET_DIR = "client_secret.json"; //TODO handle this
    private Drive service;

    public GoogleDriveHelper()  {
        // Build a new authorized API client service.
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException | IOException | URISyntaxException e) {
            logger.error("Cannot initialize Google Drive helper", e);
            throw new RuntimeException("Cannot initialize Google Drive helper");
        }
    }

    @Override
    public void run() {
        uploadFile();
    }

    public static void main(String... args) {
       new GoogleDriveHelper().uploadFile();
    }

    private void uploadFile() {
        File fileMetadata = new File();
        fileMetadata.setName("AmericanWheatJune2018_conditioning.txt"); //TODO property
        java.io.File filePath = new java.io.File("/Users/gabriele.gattari/raspyTemp/main/src/main/brewDays/201806-AmericanWheat/american_wheat_june_2018_conditioning.txt");
        FileContent mediaContent = new FileContent( null, filePath);
        try {
            File file = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            logger.info("File ID: " + file.getId());
        } catch (IOException e) {
            logger.error("cannot upload file to Google Drive", e);
        }
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException, URISyntaxException {
        // Load client secrets.
        //InputStream in = GoogleDriveHelper.class.getResourceAsStream(CLIENT_SECRET_DIR);
        Path dashboardTemplateFile = Paths.get(getClass().getClassLoader().getResource(CLIENT_SECRET_DIR).toURI());

        InputStream in = Files.newInputStream(dashboardTemplateFile); //TODO try

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(CREDENTIALS_FOLDER)))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
}
