package com.mtcrm.storage;

import com.mtcrm.activity.ActivityService;
import com.mtcrm.common.api.PageResponse;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private final ObjectStorage storage;
    private final StoredFileRepository files;
    private final StorageQuotaService quotas;
    private final ActivityService activities;
    private final Semaphore uploadSlots;

    public FileService(ObjectStorage storage, StoredFileRepository files, StorageQuotaService quotas,
                       ActivityService activities,
                       @Value("${app.storage.max-concurrent-uploads:4}") int maxConcurrentUploads) {
        this.storage = storage;
        this.files = files;
        this.quotas = quotas;
        this.activities = activities;
        this.uploadSlots = new Semaphore(Math.max(1, maxConcurrentUploads), true);
    }

    public ObjectStorage.StoredObject upload(MultipartFile file) {
        LocalObjectStorage.validate(file);
        boolean acquired;
        try { acquired = uploadSlots.tryAcquire(2, TimeUnit.SECONDS); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new BadRequestException("Upload was interrupted"); }
        if (!acquired) throw new BadRequestException("Too many uploads are in progress; try again shortly");

        UUID tenantId = TenantContext.require();
        String key = StorageKeyFactory.create(file);
        StoredFile reservation = null;
        try {
            reservation = quotas.reserve(file, key);
            ObjectStorage.StoredObject stored = storage.store(file, key);
            quotas.activate(reservation.getId(), tenantId);
            try { activities.record("UPLOADED", "FILE", reservation.getId(), "Uploaded " + reservation.getFilename()); }
            catch (RuntimeException ex) { log.warn("Upload activity could not be recorded: {}", ex.getMessage()); }
            return stored;
        } catch (RuntimeException ex) {
            safeDelete(key);
            if (reservation != null) quotas.fail(reservation.getId(), tenantId);
            throw ex;
        } finally {
            uploadSlots.release();
        }
    }

    public void delete(String key) {
        UUID tenantId = TenantContext.require();
        StoredFile metadata = files.findByTenantIdAndObjectKeyAndStatus(tenantId, key, StoredFile.Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("File not found"));
        storage.delete(key);
        quotas.markDeleted(metadata.getId(), tenantId);
        try { activities.record("DELETED", "FILE", metadata.getId(), "Deleted file"); }
        catch (RuntimeException ex) { log.warn("Delete activity could not be recorded: {}", ex.getMessage()); }
    }

    @Transactional(readOnly = true)
    public PageResponse<FileSummary> list(int page, int size) {
        if (page < 0) throw new BadRequestException("page must be zero or greater");
        if (size < 1 || size > 100) throw new BadRequestException("size must be between 1 and 100");
        var result = files.findAllByTenantIdAndStatus(TenantContext.require(), StoredFile.Status.ACTIVE,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, FileSummary::from);
    }

    @Transactional(readOnly = true)
    public FileDetails get(UUID id) {
        StoredFile file = files.findByIdAndTenantIdAndStatus(id, TenantContext.require(), StoredFile.Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("File not found"));
        return new FileDetails(file.getId(), file.getObjectKey(), file.getFilename(), file.getContentType(),
                file.getSizeBytes(), file.getCreatedAt(), storage.downloadUrl(file.getObjectKey()));
    }

    @Transactional(readOnly = true)
    public Path localContent(String tenantId, String filename) {
        if (!(storage instanceof LocalObjectStorage local)) throw new NotFoundException("File not found");
        UUID currentTenant = TenantContext.require();
        if (!currentTenant.toString().equals(tenantId)) throw new NotFoundException("File not found");
        String key = tenantId + "/" + filename;
        if (!files.existsByTenantIdAndObjectKeyAndStatus(currentTenant, key, StoredFile.Status.ACTIVE))
            throw new NotFoundException("File not found");
        return local.content(tenantId, filename);
    }

    private void safeDelete(String key) {
        try { storage.delete(key); }
        catch (RuntimeException cleanupError) { log.warn("Could not clean up upload {}: {}", key, cleanupError.getMessage()); }
    }

    public record FileSummary(UUID id, String key, String filename, String contentType, long size,
                              Instant createdAt) {
        static FileSummary from(StoredFile file) {
            return new FileSummary(file.getId(), file.getObjectKey(), file.getFilename(), file.getContentType(),
                    file.getSizeBytes(), file.getCreatedAt());
        }
    }

    public record FileDetails(UUID id, String key, String filename, String contentType, long size,
                              Instant createdAt, String url) {}
}
