package com.mtcrm.storage;

import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.config.AppProperties;
import com.mtcrm.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalObjectStorageTest {
    @TempDir Path directory;
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test
    void storesUnderTenantPrefixAndBlocksCrossTenantRead() throws Exception {
        UUID ownerTenant = UUID.randomUUID();
        TenantContext.set(ownerTenant);
        var storage = new LocalObjectStorage(properties(directory));
        var upload = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
        var stored = storage.store(upload, ownerTenant + "/" + UUID.randomUUID() + "-avatar.png");
        assertThat(stored.key()).startsWith(ownerTenant + "/");
        assertThat(Files.exists(directory.resolve(stored.key()))).isTrue();

        TenantContext.set(UUID.randomUUID());
        String filename = stored.key().substring(stored.key().indexOf('/') + 1);
        assertThatThrownBy(() -> storage.content(ownerTenant.toString(), filename))
                .isInstanceOf(com.mtcrm.common.exception.NotFoundException.class).hasMessage("File not found");
    }

    @Test
    void rejectsTraversalEvenWhenKeyStartsWithCurrentTenant() {
        UUID tenant = UUID.randomUUID();
        TenantContext.set(tenant);
        var storage = new LocalObjectStorage(properties(directory));

        assertThatThrownBy(() -> storage.delete(tenant + "/../other-tenant/private.pdf"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Invalid file key");
        assertThatThrownBy(() -> storage.content(tenant.toString(), "..\\private.pdf"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Invalid file key");
    }

    private static AppProperties properties(Path path) {
        return new AppProperties(new AppProperties.Jwt("test-secret-longer-than-thirty-two-bytes-0123456789",
                Duration.ofMinutes(15), Duration.ofDays(30), Duration.ofMinutes(30), "test"),
                new AppProperties.Cookie(false, "Lax", ""), new AppProperties.Cors(List.of("http://localhost")),
                new AppProperties.Mail(false, "test@localhost"),
                new AppProperties.Storage("local", path.toString(), "http://localhost:8080",
                        new AppProperties.Storage.S3("", "ap-south-1", "")), new AppProperties.Demo(false, ""));
    }
}
