
package com.techwithden.backend;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@org.springframework.stereotype.Service
public class Service {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SERVICE_ACCOUNT_KEY_PATH = getPathToGoogleCredentials();

    private static String getPathToGoogleCredentials() {
        String currentDirectory = System.getProperty("user.dir");
        Path filePath = Paths.get(currentDirectory, "cred.json");
        return filePath.toString();
    }

    public Res uploadImageToDrive(java.io.File file) throws GeneralSecurityException, IOException {
        Res res = new Res();

        try {
            String folderId = "1EsaafFyTpjhQ60CuDMlKkrtlXu0uxswp"; // Replace with your folder ID
            Drive drive = createDriveService();
            com.google.api.services.drive.model.File fileMetaData = new com.google.api.services.drive.model.File();
            fileMetaData.setName(file.getName());
            fileMetaData.setParents(Collections.singletonList(folderId));
            FileContent mediaContent = new FileContent("image/jpeg", file);

            // Upload the file to Google Drive
            com.google.api.services.drive.model.File uploadedFile = drive.files().create(fileMetaData, mediaContent)
                    .setFields("id").execute();

            String imageUrl = "https://drive.google.com/uc?export=view&id=" + uploadedFile.getId();
            System.out.println("IMAGE URL: " + imageUrl);

            // Delete the local file after successful upload
            if (file.delete()) {
                System.out.println("Local file deleted successfully.");
            } else {
                System.out.println("Failed to delete the local file.");
            }

            res.setStatus(200);
            res.setMessage("Image Successfully Uploaded To Drive");
            res.setUrl(imageUrl);
        } catch (Exception e) {
            System.out.println("Error during upload: " + e.getMessage());
            res.setStatus(500);
            res.setMessage(e.getMessage());
        }
        return res;
    }

    public List<File> searchFilesByName(String fileName) throws GeneralSecurityException, IOException {
        List<File> result = new ArrayList<>();
        Drive drive = createDriveService();

        try {
            String query = "name contains '" + fileName + "'";
            FileList fileList = drive.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            result = fileList.getFiles();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
        return result;
    }

    public void deleteFileFromDrive(String fileId) throws GeneralSecurityException, IOException {
        Drive drive = createDriveService();
        try {
            drive.files().delete(fileId).execute();
            System.out.println("File deleted from Google Drive successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while deleting the file: " + e.getMessage());
        }
    }

    private Drive createDriveService() throws GeneralSecurityException, IOException {
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(SERVICE_ACCOUNT_KEY_PATH))
                .createScoped(Collections.singleton(DriveScopes.DRIVE));

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .build();
    }
}