package com.mtcrm.auth;

import com.mtcrm.config.AppProperties;
import com.mtcrm.notification.MailService;
import com.mtcrm.security.JwtService;
import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.tenant.TenantQuotaService;
import com.mtcrm.user.AppUser;
import com.mtcrm.user.Role;
import com.mtcrm.user.UserRepository;
import com.mtcrm.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock TenantRepository tenants;
    @Mock UserRepository users;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock ActionTokenRepository actionTokens;
    @Mock PendingRegistrationRepository pendingRegistrations;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwt;
    @Mock MailService mail;
    @Mock TenantQuotaService quotas;

    @Test
    void registrationStaysPendingUntilMailboxProof() {
        when(passwordEncoder.encode("StrongPassword123")).thenReturn("bcrypt-hash");
        var service = new AuthService(tenants, users, refreshTokens, actionTokens, pendingRegistrations,
                passwordEncoder, jwt, properties(), mail, quotas);

        service.register(new AuthDtos.RegisterRequest("Example Inc", "Alex", "Morgan",
                "Alex@Example.Test", "StrongPassword123"));

        ArgumentCaptor<PendingRegistration> saved = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrations).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("alex@example.test");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        verify(quotas).assertCanCreatePendingRegistration();
        verify(tenants, never()).save(any());
        verify(users, never()).save(any());
    }

    @Test
    void verificationAtomicallyCreatesTheTenantAndVerifiedAdministrator() {
        String raw = "pending-token";
        UUID tenantId = UUID.randomUUID();
        PendingRegistration pending = new PendingRegistration();
        pending.setCompanyName("Example Inc"); pending.setFirstName("Alex"); pending.setLastName("Morgan");
        pending.setEmail("alex@example.test"); pending.setPasswordHash("bcrypt-hash");
        pending.setTokenHash(AuthService.hash(raw)); pending.setTokenExpiresAt(Instant.now().plusSeconds(600));
        when(pendingRegistrations.findByTokenHash(AuthService.hash(raw))).thenReturn(Optional.of(pending));
        when(pendingRegistrations.findByTokenHashForUpdate(AuthService.hash(raw))).thenReturn(Optional.of(pending));
        when(tenants.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0); tenant.setId(tenantId); return tenant;
        });
        var service = new AuthService(tenants, users, refreshTokens, actionTokens, pendingRegistrations,
                passwordEncoder, jwt, properties(), mail, quotas);

        service.verifyEmail(raw);

        ArgumentCaptor<AppUser> admin = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(admin.capture());
        assertThat(admin.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(admin.getValue().getRole()).isEqualTo(Role.COMPANY_ADMIN);
        assertThat(admin.getValue().getEmailVerifiedAt()).isNotNull();
        verify(quotas).assertCanRegisterTenant();
        verify(pendingRegistrations).delete(pending);
    }

    @Test
    void meReturnsProfileWithoutMintingAnotherAccessToken() {
        UUID tenantId = UUID.randomUUID(), userId = UUID.randomUUID();
        AppUser user = new AppUser(); user.setId(userId); user.setTenantId(tenantId); user.setFirstName("Alex");
        user.setLastName("Morgan"); user.setEmail("alex@example.test"); user.setRole(Role.COMPANY_ADMIN);
        Tenant tenant = new Tenant(); tenant.setId(tenantId); tenant.setName("Example Inc"); tenant.setSlug("example");
        when(users.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));
        var service = new AuthService(tenants, users, refreshTokens, actionTokens, pendingRegistrations,
                passwordEncoder, jwt, properties(), mail, quotas);

        AuthDtos.UserSummary summary = service.me(userId, tenantId);

        assertThat(summary.tenantName()).isEqualTo("Example Inc");
        verify(jwt, never()).createAccessToken(any());
    }

    @Test
    void refreshRotatesPersistedOpaqueToken() {
        UUID tenantId = UUID.randomUUID(), userId = UUID.randomUUID();
        AppUser user = new AppUser(); user.setId(userId); user.setTenantId(tenantId); user.setFirstName("Alex");
        user.setLastName("Morgan"); user.setEmail("alex@example.test"); user.setRole(Role.COMPANY_ADMIN); user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        Tenant tenant = new Tenant(); tenant.setId(tenantId); tenant.setName("Example"); tenant.setSlug("example");
        RefreshToken old = new RefreshToken(); old.setTenantId(tenantId); old.setUserId(userId);
        old.setFamilyId(UUID.randomUUID());
        old.setTokenHash(AuthService.hash("old-token")); old.setExpiresAt(Instant.now().plusSeconds(3600));
        when(refreshTokens.findByTokenHash(AuthService.hash("old-token"))).thenReturn(Optional.of(old));
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(refreshTokens.findByTokenHashForUpdate(AuthService.hash("old-token"))).thenReturn(Optional.of(old));
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(jwt.createAccessToken(user)).thenReturn("new-access"); when(jwt.accessExpiresInSeconds()).thenReturn(900L);
        var service = new AuthService(tenants, users, refreshTokens, actionTokens, pendingRegistrations,
                passwordEncoder, jwt, properties(), mail, quotas);

        AuthService.Session session = service.refresh("old-token", "test-agent", "127.0.0.1");

        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(old.getReplacedByHash()).isEqualTo(AuthService.hash(session.refreshToken()));
        assertThat(session.response().accessToken()).isEqualTo("new-access");
        ArgumentCaptor<RefreshToken> created = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(created.capture());
        assertThat(created.getValue().getTokenHash()).isEqualTo(AuthService.hash(session.refreshToken()));
        assertThat(created.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(created.getValue().getExpiresAt()).isEqualTo(old.getExpiresAt());
    }

    @Test
    void replayingASecurityRevokedTokenCannotRevokeANewerSessionFamily() {
        UUID familyId = UUID.randomUUID(), userId = UUID.randomUUID(), tenantId = UUID.randomUUID();
        RefreshToken old = new RefreshToken(); old.setFamilyId(familyId); old.setUserId(userId); old.setTenantId(tenantId);
        old.setTokenHash(AuthService.hash("revoked-token")); old.setExpiresAt(Instant.now().plusSeconds(3600));
        old.setRevokedAt(Instant.now());
        AppUser user = new AppUser(); user.setId(userId); user.setTenantId(tenantId);
        when(refreshTokens.findByTokenHash(AuthService.hash("revoked-token"))).thenReturn(Optional.of(old));
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(refreshTokens.findByTokenHashForUpdate(AuthService.hash("revoked-token"))).thenReturn(Optional.of(old));
        var service = new AuthService(tenants, users, refreshTokens, actionTokens, pendingRegistrations,
                passwordEncoder, jwt, properties(), mail, quotas);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.refresh("revoked-token", "agent", "127.0.0.1"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);

        verify(refreshTokens, org.mockito.Mockito.never()).revokeAllForFamily(any(), any());
    }

    @Test
    void issuingANewPasswordResetInvalidatesOlderLinks() {
        UUID tenantId = UUID.randomUUID(), userId = UUID.randomUUID();
        AppUser user = new AppUser(); user.setId(userId); user.setTenantId(tenantId); user.setFirstName("Alex");
        user.setLastName("Morgan"); user.setEmail("alex@example.test"); user.setRole(Role.EMPLOYEE); user.setStatus(UserStatus.ACTIVE);
        when(users.findByEmailIgnoreCase("alex@example.test")).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        Tenant tenant = new Tenant(); tenant.setId(tenantId); tenant.setName("Example"); tenant.setSlug("example");
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));
        var service = new AuthService(tenants, users, refreshTokens, actionTokens, pendingRegistrations,
                passwordEncoder, jwt, properties(), mail, quotas);

        service.forgotPassword("Alex@Example.Test");

        verify(actionTokens).invalidateUnusedForUser(org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(ActionToken.Type.PASSWORD_RESET), any(Instant.class));
        verify(actionTokens).save(any(ActionToken.class));
    }

    @Test
    void resettingPasswordInvalidatesEverySiblingResetLink() {
        UUID tenantId = UUID.randomUUID(), userId = UUID.randomUUID();
        AppUser user = new AppUser(); user.setId(userId); user.setTenantId(tenantId); user.setFirstName("Alex");
        user.setLastName("Morgan"); user.setEmail("alex@example.test"); user.setRole(Role.EMPLOYEE); user.setStatus(UserStatus.ACTIVE);
        ActionToken token = new ActionToken(); token.setTenantId(tenantId); token.setUserId(userId);
        token.setType(ActionToken.Type.PASSWORD_RESET); token.setTokenHash(AuthService.hash("reset-token"));
        token.setExpiresAt(Instant.now().plusSeconds(600));
        when(actionTokens.findByTokenHashAndType(AuthService.hash("reset-token"), ActionToken.Type.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(actionTokens.findByTokenHashAndTypeForUpdate(AuthService.hash("reset-token"), ActionToken.Type.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        Tenant tenant = new Tenant(); tenant.setId(tenantId); tenant.setName("Example"); tenant.setSlug("example");
        when(tenants.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(passwordEncoder.encode("StrongerPass123")).thenReturn("encoded");
        var service = new AuthService(tenants, users, refreshTokens, actionTokens, pendingRegistrations,
                passwordEncoder, jwt, properties(), mail, quotas);

        service.resetPassword("reset-token", "StrongerPass123");

        assertThat(user.getSessionVersion()).isEqualTo(1);
        verify(actionTokens).invalidateUnusedForUser(org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(ActionToken.Type.PASSWORD_RESET), any(Instant.class));
        verify(refreshTokens).revokeAllForUser(org.mockito.ArgumentMatchers.eq(userId), any(Instant.class));
    }

    private static AppProperties properties() {
        return new AppProperties(new AppProperties.Jwt("test-secret-longer-than-thirty-two-bytes-0123456789",
                Duration.ofMinutes(15), Duration.ofDays(30), Duration.ofMinutes(30), "test"),
                new AppProperties.Cookie(false, "Lax", ""), new AppProperties.Cors(List.of("http://localhost")),
                new AppProperties.Mail(false, "test@localhost"),
                new AppProperties.Storage("local", "./target/test", "http://localhost",
                        new AppProperties.Storage.S3("", "ap-south-1", "")), new AppProperties.Demo(false, ""));
    }
}
