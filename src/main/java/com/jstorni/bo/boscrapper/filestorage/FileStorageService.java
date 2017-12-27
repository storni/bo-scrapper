package com.jstorni.bo.boscrapper.filestorage;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    String write(String fileIdentifier, InputStream data);

    InputStream read(String fileIdentifier);

}
