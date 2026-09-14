package com.mtcrm.security;

import com.mtcrm.config.AppProperties;
import com.mtcrm.user.AppUser;
import com.mtcrm.user.Role;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void accessTokenCarriesSignedUserAndTenantIdentity() {
        JwtService service = new JwtService(properties());
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId); user.setTenantId(tenantId); user.setEmail("admin@example.test"); user.setRole(Role.COMPANY_ADMIN);

        CurrentUser parsed = service.parseAccessToken(service.createAccessToken(user));

        assertThat(parsed.id()).isEqualTo(userId);
        assertThat(parsed.tenantId()).isEqualTo(tenantId);
        assertThat(parsed.email()).isEqualTo("admin@example.test");
        assertThat(parsed.role()).isEqualTo(Role.COMPANY_ADMIN);
    }

    private static AppProperties properties() {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-longer-than-thirty-two-bytes-0123456789", Duration.ofMinutes(15),
                        Duration.ofDays(30), Duration.ofMinutes(30), "mt-crm-test"),
                new AppProperties.Cookie(false, "Lax", ""), new AppProperties.Cors(List.of("http://localhost")),
                new AppProperties.Mail(false, "test@localhost"),
                new AppProperties.Storage("local", "./target/test", "http://localhost", new AppProperties.Storage.S3("", "ap-south-1", "")),
                new AppProperties.Demo(false, ""));
    }
}
