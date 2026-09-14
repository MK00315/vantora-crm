package com.mtcrm.storage;

import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class StorageQuotaService {
    private final StoredFileRepository files;
    private final StorageQuotaLockRepository quotaLocks;
    private final TenantRepository tenants;
    private final long maxBytesPerTenant;
    private final int maxFilesPerTenant;
    private final int maxUploadsPerHour;
    private final long maxBytesTotal;
    private final int maxFilesTotal;
    private final int maxUploadsPerHourTotal;

    public StorageQuotaService(StoredFileRepository files, StorageQuotaLockRepository quotaLocks,
                               TenantRepository tenants,
                               @Value("${app.storage.max-bytes-per-tenant:104857600}") long maxBytesPerTenant,
                               @Value("${app.storage.max-files-per-tenant:1000}") int maxFilesPerTenant,
                               @Value("${app.storage.max-uploads-per-hour:60}") int maxUploadsPerHour,
                               @Value("${app.storage.max-bytes-total:1073741824}") long maxBytesTotal,
                               @Value("${app.storage.max-files-total:10000}") int maxFilesTotal,
                               @Value("${app.storage.max-uploads-per-hour-total:1000}") int maxUploadsPerHourTotal) {
        this.files = files;
        this.quotaLocks = quotaLocks;
        this.tenants = tenants;
        this.maxBytesPerTenant = maxBytesPerTenant;
        this.maxFilesPerTenant = maxFilesPerTenant;
        this.maxUploadsPerHour = maxUploadsPerHour;
        this.maxBytesTotal = maxBytesTotal;
        this.maxFilesTotal = maxFilesTotal;
        this.maxUploadsPerHourTotal = maxUploadsPerHourTotal;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StoredFile reserve(MultipartFile file, String key) {
        UUID tenantId = TenantContext.require();
        quotaLocks.findByIdForUpdate(1).orElseThrow(() -> new IllegalStateException("Storage quota guard is missing"));
        tenants.findByIdForUpdate(tenantId).orElseThrow(() -> new NotFoundException("Tenant not found"));
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        if (files.countByDeletedAtIsNull() >= maxFilesTotal)
            throw new BadRequestException("Deployment file-count quota has been reached");
        requireCapacity(files.totalBytes(), maxBytesTotal, file.getSize(), "Deployment storage quota has been reached");
        if (files.countByCreatedAtAfter(oneHourAgo) >= maxUploadsPerHourTotal)
            throw new BadRequestException("Deployment hourly upload limit has been reached");
        if (files.countByTenantIdAndDeletedAtIsNull(tenantId) >= maxFilesPerTenant)
            throw new BadRequestException("Workspace file-count quota has been reached");
        requireCapacity(files.totalBytesByTenantId(tenantId), maxBytesPerTenant, file.getSize(),
                "Workspace storage quota has been reached");
        if (files.countByTenantIdAndCreatedAtAfter(tenantId, oneHourAgo) >= maxUploadsPerHour)
            throw new BadRequestException("Workspace hourly upload limit has been reached");

        StoredFile metadata = new StoredFile();
        metadata.setTenantId(tenantId);
        metadata.setObjectKey(key);
        metadata.setFilename(StorageKeyFactory.originalFilename(file));
        metadata.setContentType(file.getContentType());
        metadata.setSizeBytes(file.getSize());
        metadata.setStatus(StoredFile.Status.PENDING);
        return files.saveAndFlush(metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activate(UUID id, UUID tenantId) {
        StoredFile metadata = files.findByIdAndTenantIdAndStatus(id, tenantId, StoredFile.Status.PENDING)
                .orElseThrow(() -> new NotFoundException("Upload reservation not found"));
        metadata.setStatus(StoredFile.Status.ACTIVE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID id, UUID tenantId) {
        files.findByIdAndTenantIdAndStatus(id, tenantId, StoredFile.Status.PENDING).ifPresent(metadata -> {
            metadata.setStatus(StoredFile.Status.FAILED);
            metadata.setDeletedAt(Instant.now());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDeleted(UUID id, UUID tenantId) {
        StoredFile metadata = files.findByIdAndTenantIdAndStatus(id, tenantId, StoredFile.Status.ACTIVE)
                .orElseThrow(() -> new NotFoundException("File not found"));
        metadata.setStatus(StoredFile.Status.DELETED);
        metadata.setDeletedAt(Instant.now());
    }

    private static void requireCapacity(long used, long maximum, long requested, String message) {
        if (requested > maximum - Math.min(used, maximum)) throw new BadRequestException(message);
    }
}
