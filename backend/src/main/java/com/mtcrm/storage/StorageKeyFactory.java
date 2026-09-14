package com.mtcrm.storage;

import com.mtcrm.tenant.TenantContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

final class StorageKeyFactory {
    private StorageKeyFactory() {}

    static String create(MultipartFile file) {
        String clean = originalFilename(file).replaceAll("[^a-zA-Z0-9._-]", "_");
        clean = clean.substring(0, Math.min(clean.length(), 120));
        return TenantContext.require() + "/" + UUID.randomUUID() + "-" + clean;
    }

    static String originalFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) return "upload";
        return original.substring(0, Math.min(original.length(), 255));
    }
}
