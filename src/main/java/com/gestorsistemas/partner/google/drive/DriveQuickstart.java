package com.gestorsistemas.partner.google.drive;

/**
 * @author Jeferson Almeida
 */

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class DriveQuickstart {
    private static final Logger logger = LoggerFactory.getLogger(DriveQuickstart.class);
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String DOWNLOAD_DIRECTORY_PATH = "downloads";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credential-google-drive.json";
    private static Drive service;

    public static void main(String... args) throws IOException {
        // Build a new authorized API client service.
        service = getGoogleDriveService();
        String extensao = ".xml";
        String pageToken = null;

        do {
            FileList result = searchFiles(service, extensao, pageToken);
            List<File> files = result.getFiles();
            if (files == null || files.isEmpty()) {
                logger.info("Não foram encontrado arquivos.");
            } else {
                logger.info("Arquivos:");
                for (File file : files) {
                    processarArquivoDrive(service, extensao, file);
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
    }

    private static FileList searchFiles(Drive service, String extensao, String pageToken) throws IOException {
        return service.files().list()
                .setQ("name contains '" + extensao + "'")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .setPageToken(pageToken)
                .execute();
    }

    private static void downloadArquivo(Drive service, File file, java.io.File arquivo) throws IOException {
        try (OutputStream out = new FileOutputStream(arquivo)) {
            final Drive.Files.Get request = service.files().get(file.getId());
            request.getMediaHttpDownloader().setProgressListener(new FileDownloadProgressListener());
//            request.executeAndDownloadTo(out);
            request.executeMediaAndDownloadTo(out);
        }
    }

    private static void processarArquivoDrive(Drive service, String extensao, File file) throws IOException {
        logger.info("{} {}", file.getName(), file.getId());
        java.io.File parentDir = new java.io.File(DOWNLOAD_DIRECTORY_PATH);
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            logger.error("Não foi possível criar o diretório");
            throw new IOException("Não foi possível criar o diretório");
        }
        final java.io.File arquivo = new java.io.File(parentDir, file.getName());
        if (!arquivo.exists()) {
            if (extensao.replace(".", "").equals(FilenameUtils.getExtension((file.getName())))) {
                downloadArquivo(service, file, arquivo);
            } else {
                logger.info("Arquivo não é o esperado: {}", file.getName());
                logger.info("Extensão: {}", FilenameUtils.getExtension(file.getName()));
            }
        } else {
            logger.info("Excluíndo arquivo: {}", file.getName());
//            service.files().delete(file.getId()).execute();
        }
    }

    public static Drive getGoogleDriveService(){
        if(service == null) {
            NetHttpTransport HTTP_TRANSPORT;
            try {
                HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

                service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
            } catch (GeneralSecurityException | IOException e) {
                logger.error("Erro ao configurar serviço.", e);
            }
        }
        return service;
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = DriveQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                clientSecrets,
                SCOPES
        )
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        //returns an authorized Credential object.
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
