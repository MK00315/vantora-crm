package com.mtcrm.storage;

import com.mtcrm.tenant.TenantContext;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class StorageReservationCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(StorageReservationCleanupJob.class);
    private final StoredFileRepository files;
    private final ObjectStorage storage;
    private final StorageQuotaService quotas;
    private final Duration reservationTtl;

    public StorageReservationCleanupJob(StoredFileRepository files, ObjectStorage storage, StorageQuotaService quotas,
                                        @Value("${app.storage.reservation-ttl:1h}") Duration reservationTtl) {
        this.files = files;
        this.storage = storage;
        this.quotas = quotas;
        this.reservationTtl = reservationTtl;
    }

    @Scheduled(cron = "${app.storage.cleanup-cron:0 */15 * * * *}")
    @SchedulerLock(name = "storage-reservation-cleanup", lockAtMostFor = "10m", lockAtLeastFor = "5s")
    public void cleanAbandonedReservations() {
        var expired = files.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                StoredFile.Status.PENDING, Instant.now().minus(reservationTtl));
        for (StoredFile file : expired) {
            TenantContext.set(file.getTenantId());
            try {
                storage.delete(file.getObjectKey());
                quotas.fail(file.getId(), file.getTenantId());
            } catch (RuntimeException ex) {
                log.warn("Could not clean abandoned upload {}: {}", file.getId(), ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
