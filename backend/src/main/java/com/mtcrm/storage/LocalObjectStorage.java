package com.mtcrm.storage;

import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.config.AppProperties;
import com.mtcrm.tenant.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStorage {
    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf", "text/csv");
    private final Path base;
    private final String publicBase;

    public LocalObjectStorage(AppProperties properties) {
        this.base = Path.of(properties.storage().localPath()).toAbsolutePath().normalize();
        this.publicBase = properties.storage().publicBaseUrl().replaceAll("/$", "");
    }

    @Override
    public StoredObject store(MultipartFile file, String key) {
        validate(file);
        String original = StorageKeyFactory.originalFilename(file);
        Path destination = base.resolve(key).normalize();
        tenantPath(key);
        if (!destination.startsWith(base)) throw new BadRequestException("Invalid filename");
        try {
            Files.createDirectories(destination.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) { throw new IllegalStateException("Could not store file", ex); }
        return new StoredObject(key, downloadUrl(key),
                original, file.getContentType(), file.getSize());
    }

    @Override
    public void delete(String key) {
        Path target = tenantPath(key);
        try { Files.deleteIfExists(target); }
        catch (IOException ex) { throw new IllegalStateException("Could not delete file", ex); }
    }

    @Override
    public String downloadUrl(String key) {
        Path target = tenantPath(key);
        if (!Files.isRegularFile(target)) throw new NotFoundException("File not found");
        return publicBase + "/api/v1/files/content/" + key;
    }

    public Path content(String tenantId, String filename) {
        if (!TenantContext.require().toString().equals(tenantId))
            throw new NotFoundException("File not found");
        return tenantPath(tenantId + "/" + filename);
    }

    private Path tenantPath(String key) {
        Path tenantRoot = base.resolve(TenantContext.require().toString()).normalize();
        String prefix = TenantContext.require() + "/";
        if (!key.startsWith(prefix)) throw new NotFoundException("File not found");
        String filename = key.substring(prefix.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.equals(".") || filename.equals(".."))
            throw new BadRequestException("Invalid file key");
        Path target = tenantRoot.resolve(filename).normalize();
        if (!target.startsWith(tenantRoot) || !tenantRoot.equals(target.getParent()))
            throw new BadRequestException("Invalid file key");
        return target;
    }

    static void validate(MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("File is empty");
        if (file.getSize() > MAX_SIZE) throw new BadRequestException("File must be 10 MB or smaller");
        if (!ALLOWED.contains(file.getContentType())) throw new BadRequestException("Unsupported file type");
    }
}
