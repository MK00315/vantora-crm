package com.mtcrm.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorage {
    StoredObject store(MultipartFile file, String key);
    void delete(String key);
    String downloadUrl(String key);

    record StoredObject(String key, String url, String filename, String contentType, long size) {}
}
