package com.jstorni.bo.boscrapper.filestorage;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class LocalFileSystemStorage implements FileStorageService {

    private final String basePath;

    public LocalFileSystemStorage(@Value("${boscrapper.localstorage.base-path}") String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String write(String fileIdentifier, InputStream data) {
        String fileLocation = basePath + fileIdentifier;
        File contentFile = new File(fileLocation);
        if (contentFile.exists() && !FileUtils.deleteQuietly(contentFile)) {
            throw new IllegalArgumentException("Error while deleting existing content file" + fileLocation);
        }

        try {
            FileUtils.copyInputStreamToFile(data, contentFile);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Error while writing content to file " + fileLocation, ex);
        }

        return fileLocation;
    }

    @Override
    public InputStream read(String fileIdentifier) {
        String fileLocation = basePath + fileIdentifier;
        try {
            return new FileInputStream(fileLocation);
        } catch (FileNotFoundException ex) {
            throw new IllegalArgumentException("File not found " + fileLocation, ex);
        }

    }
}
