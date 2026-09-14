package com.mtcrm.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
    long countByTenantIdAndCreatedAtAfter(UUID tenantId, Instant createdAfter);
    long countByDeletedAtIsNull();
    long countByCreatedAtAfter(Instant createdAfter);
    Optional<StoredFile> findByTenantIdAndObjectKeyAndStatus(UUID tenantId, String objectKey, StoredFile.Status status);
    Optional<StoredFile> findByIdAndTenantIdAndStatus(UUID id, UUID tenantId, StoredFile.Status status);
    boolean existsByTenantIdAndObjectKeyAndStatus(UUID tenantId, String objectKey, StoredFile.Status status);
    Page<StoredFile> findAllByTenantIdAndStatus(UUID tenantId, StoredFile.Status status, Pageable pageable);
    java.util.List<StoredFile> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(StoredFile.Status status,
                                                                                     Instant createdBefore);

    @Query("select coalesce(sum(file.sizeBytes), 0) from StoredFile file where file.tenantId = :tenantId and file.deletedAt is null")
    long totalBytesByTenantId(@Param("tenantId") UUID tenantId);

    @Query("select coalesce(sum(file.sizeBytes), 0) from StoredFile file where file.deletedAt is null")
    long totalBytes();
}
