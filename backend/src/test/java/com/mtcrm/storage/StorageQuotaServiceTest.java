package com.mtcrm.storage;

import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageQuotaServiceTest {
    @Mock StoredFileRepository files;
    @Mock StorageQuotaLockRepository quotaLocks;
    @Mock TenantRepository tenants;

    @AfterEach
    void clearTenant() { TenantContext.clear(); }

    @Test
    void rejectsAnUploadThatWouldExceedTheTenantByteQuota() {
        UUID tenantId = prepareTenant();
        when(files.totalBytesByTenantId(tenantId)).thenReturn(95L);
        var upload = new MockMultipartFile("file", "data.csv", "text/csv", new byte[10]);

        assertThatThrownBy(() -> service().reserve(upload, tenantId + "/file.csv"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("storage quota");
        verify(files, never()).saveAndFlush(any());
    }

    @Test
    void deletedFilesStillCountTowardTheHourlyOperationLimit() {
        UUID tenantId = prepareTenant();
        when(files.countByTenantIdAndCreatedAtAfter(org.mockito.ArgumentMatchers.eq(tenantId), any()))
                .thenReturn(10L);
        var upload = new MockMultipartFile("file", "data.csv", "text/csv", new byte[1]);

        assertThatThrownBy(() -> service().reserve(upload, tenantId + "/file.csv"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("hourly upload limit");
        verify(files, never()).saveAndFlush(any());
    }

    private UUID prepareTenant() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        when(quotaLocks.findByIdForUpdate(1)).thenReturn(Optional.of(new StorageQuotaLock()));
        when(tenants.findByIdForUpdate(tenantId)).thenReturn(Optional.of(new Tenant()));
        return tenantId;
    }

    private StorageQuotaService service() {
        return new StorageQuotaService(files, quotaLocks, tenants, 100, 10, 10, 1000, 100, 100);
    }
}
