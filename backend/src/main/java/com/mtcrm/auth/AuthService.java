package com.mtcrm.auth;

import com.mtcrm.auth.AuthDtos.AuthResponse;
import com.mtcrm.auth.AuthDtos.UserSummary;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.common.exception.ConflictException;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final java.time.Duration PENDING_REGISTRATION_TTL = java.time.Duration.ofHours(24);
    private final TenantRepository tenants;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final ActionTokenRepository actionTokens;
    private final PendingRegistrationRepository pendingRegistrations;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final MailService mailService;
    private final TenantQuotaService quotas;

    public AuthService(TenantRepository tenants, UserRepository users, RefreshTokenRepository refreshTokens,
                       ActionTokenRepository actionTokens, PendingRegistrationRepository pendingRegistrations,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       AppProperties properties, MailService mailService, TenantQuotaService quotas) {
        this.tenants = tenants;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.actionTokens = actionTokens;
        this.pendingRegistrations = pendingRegistrations;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.mailService = mailService;
        this.quotas = quotas;
    }

    @Transactional
    public void register(AuthDtos.RegisterRequest request) {
        String email = normalizeEmail(request.email());
        // BCrypt runs before the first database query, keeping the quota-lock transaction short.
        String passwordHash = passwordEncoder.encode(request.password());
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictException("An account with this email already exists");
        pendingRegistrations.findByEmailForUpdate(email).ifPresent(existing -> {
            boolean expired = existing.getCreatedAt() != null
                    && existing.getCreatedAt().isBefore(Instant.now().minus(PENDING_REGISTRATION_TTL));
            if (!expired) throw new ConflictException("A verification is already pending for this email");
            pendingRegistrations.delete(existing);
            pendingRegistrations.flush();
        });
        quotas.assertCanCreatePendingRegistration();

        String rawToken = opaqueToken();
        PendingRegistration pending = new PendingRegistration();
        pending.setCompanyName(request.companyName().trim());
        pending.setFirstName(request.firstName().trim());
        pending.setLastName(request.lastName().trim());
        pending.setEmail(email);
        pending.setPasswordHash(passwordHash);
        pending.setTokenHash(hash(rawToken));
        pending.setTokenExpiresAt(Instant.now().plus(properties.jwt().actionTtl()));
        pendingRegistrations.save(pending);
        mailService.welcome(pending.getEmail(), pending.getFirstName(), rawToken);
    }

    @Transactional
    public Session login(AuthDtos.LoginRequest request, String userAgent, String ip) {
        AppUser user = users.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid email or password");
        if (user.getStatus() != UserStatus.ACTIVE) throw new BadCredentialsException("This account is not active");
        if (user.getEmailVerifiedAt() == null) throw new BadCredentialsException("Verify your email before signing in");
        Tenant tenant = tenants.findById(user.getTenantId()).filter(Tenant::isActive)
                .orElseThrow(() -> new BadCredentialsException("This company is not active"));
        user.setLastLoginAt(Instant.now());
        return createSession(user, tenant, userAgent, ip);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Session refresh(String rawToken, String userAgent, String ip) {
        if (rawToken == null || rawToken.isBlank()) throw new BadCredentialsException("Refresh token is missing");
        String tokenHash = hash(rawToken);
        RefreshToken preview = refreshTokens.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token is invalid"));
        AppUser user = users.findByIdForUpdate(preview.getUserId())
                .filter(candidate -> candidate.getTenantId().equals(preview.getTenantId()))
                .orElseThrow(() -> new BadCredentialsException("User is not active"));
        RefreshToken existing = refreshTokens.findByTokenHashForUpdate(tokenHash)
                .filter(token -> token.getUserId().equals(user.getId()) && token.getTenantId().equals(user.getTenantId()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token is invalid"));
        if (!existing.isUsable(Instant.now())) {
            if (existing.getRevokedAt() != null && existing.getReplacedByHash() != null)
                refreshTokens.revokeAllForFamily(existing.getFamilyId(), Instant.now());
            throw new BadCredentialsException("Refresh token is expired or has been reused");
        }
        if (user.getStatus() != UserStatus.ACTIVE || user.getEmailVerifiedAt() == null)
            throw new BadCredentialsException("User is not active");
        Tenant tenant = tenants.findById(user.getTenantId()).filter(Tenant::isActive)
                .orElseThrow(() -> new BadCredentialsException("Company is not active"));
        existing.setRevokedAt(Instant.now());
        Session next = createSession(user, tenant, userAgent, ip, existing.getFamilyId(), existing.getExpiresAt());
        existing.setReplacedByHash(hash(next.refreshToken()));
        return next;
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        String tokenHash = hash(rawToken);
        refreshTokens.findByTokenHash(tokenHash).ifPresent(preview -> {
            users.findByIdForUpdate(preview.getUserId()).ifPresent(ignored ->
                    refreshTokens.findByTokenHashForUpdate(tokenHash)
                            .ifPresent(token -> token.setRevokedAt(Instant.now())));
        });
    }

    @Transactional
    public void revokeSessions(UUID userId) {
        AppUser user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new BadCredentialsException("User no longer exists"));
        user.incrementSessionVersion();
        refreshTokens.revokeAllForUser(userId, Instant.now());
    }

    @Transactional
    public void invalidatePasswordResets(UUID userId) {
        users.findByIdForUpdate(userId).orElseThrow(() -> new BadCredentialsException("User no longer exists"));
        actionTokens.invalidateUnusedForUser(userId, ActionToken.Type.PASSWORD_RESET, Instant.now());
    }

    @Transactional(readOnly = true)
    public UserSummary me(UUID userId, UUID tenantId) {
        AppUser user = users.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new BadCredentialsException("User no longer exists"));
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        return summary(user, tenant);
    }

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(preview -> {
            users.findByIdForUpdate(preview.getId())
                    .filter(user -> user.getStatus() != UserStatus.SUSPENDED)
                    .filter(user -> tenants.findById(user.getTenantId()).filter(Tenant::isActive).isPresent())
                    .ifPresent(user -> {
                        actionTokens.invalidateUnusedForUser(user.getId(), ActionToken.Type.PASSWORD_RESET, Instant.now());
                        String raw = createActionToken(user, ActionToken.Type.PASSWORD_RESET);
                        mailService.passwordReset(user.getEmail(), raw);
                    });
        });
    }

    @Transactional
    public void resendVerification(String email) {
        String normalized = normalizeEmail(email);
        PendingRegistration pending = pendingRegistrations.findByEmailForUpdate(normalized).orElse(null);
        if (pending != null) {
            if (pending.getCreatedAt() != null
                    && pending.getCreatedAt().isBefore(Instant.now().minus(PENDING_REGISTRATION_TTL))) {
                pendingRegistrations.delete(pending);
                return;
            }
            String raw = opaqueToken();
            pending.setTokenHash(hash(raw));
            pending.setTokenExpiresAt(Instant.now().plus(properties.jwt().actionTtl()));
            mailService.welcome(pending.getEmail(), pending.getFirstName(), raw);
            return;
        }
        users.findByEmailIgnoreCase(normalized).ifPresent(preview -> {
            users.findByIdForUpdate(preview.getId())
                    .filter(user -> user.getEmailVerifiedAt() == null && user.getStatus() == UserStatus.ACTIVE)
                    .filter(user -> tenants.findById(user.getTenantId()).filter(Tenant::isActive).isPresent())
                    .ifPresent(user -> {
                        actionTokens.invalidateUnusedForUser(user.getId(), ActionToken.Type.EMAIL_VERIFICATION, Instant.now());
                        String raw = createActionToken(user, ActionToken.Type.EMAIL_VERIFICATION);
                        mailService.welcome(user.getEmail(), user.getFirstName(), raw);
                    });
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String password) {
        LockedAction locked = lockActionToken(rawToken, ActionToken.Type.PASSWORD_RESET);
        ActionToken token = locked.token();
        AppUser user = locked.user();
        if (user.getStatus() == UserStatus.SUSPENDED
                || tenants.findById(user.getTenantId()).filter(Tenant::isActive).isEmpty())
            throw new BadRequestException("Reset token is invalid");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.incrementSessionVersion();
        if (user.getStatus() == UserStatus.INVITED) user.setStatus(UserStatus.ACTIVE);
        if (user.getEmailVerifiedAt() == null) user.setEmailVerifiedAt(Instant.now());
        Instant usedAt = Instant.now();
        actionTokens.invalidateUnusedForUser(user.getId(), ActionToken.Type.PASSWORD_RESET, usedAt);
        refreshTokens.revokeAllForUser(user.getId(), Instant.now());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = hash(rawToken);
        if (pendingRegistrations.findByTokenHash(tokenHash).isPresent()) {
            PendingRegistration pending = pendingRegistrations.findByTokenHashForUpdate(tokenHash)
                    .orElseThrow(() -> new BadRequestException("Verification token is invalid"));
            if (pending.getTokenExpiresAt().isBefore(Instant.now()))
                throw new BadRequestException("Verification token is invalid or expired");
            if (users.existsByEmailIgnoreCase(pending.getEmail())) {
                pendingRegistrations.delete(pending);
                throw new ConflictException("An account with this email already exists");
            }
            quotas.assertCanRegisterTenant();
            Tenant tenant = new Tenant();
            tenant.setName(pending.getCompanyName());
            tenant.setSlug(uniqueSlug(pending.getCompanyName()));
            tenants.save(tenant);

            AppUser user = new AppUser();
            user.setTenantId(tenant.getId());
            user.setFirstName(pending.getFirstName());
            user.setLastName(pending.getLastName());
            user.setEmail(pending.getEmail());
            user.setPasswordHash(pending.getPasswordHash());
            user.setRole(Role.COMPANY_ADMIN);
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerifiedAt(Instant.now());
            users.save(user);
            pendingRegistrations.delete(pending);
            return;
        }
        LockedAction locked = lockActionToken(rawToken, ActionToken.Type.EMAIL_VERIFICATION);
        AppUser user = locked.user();
        if (user.getStatus() != UserStatus.ACTIVE
                || tenants.findById(user.getTenantId()).filter(Tenant::isActive).isEmpty())
            throw new BadRequestException("Verification token is invalid");
        user.setEmailVerifiedAt(Instant.now());
        actionTokens.invalidateUnusedForUser(user.getId(), ActionToken.Type.EMAIL_VERIFICATION, Instant.now());
    }

    private LockedAction lockActionToken(String raw, ActionToken.Type type) {
        String tokenHash = hash(raw);
        ActionToken preview = actionTokens.findByTokenHashAndType(tokenHash, type)
                .orElseThrow(() -> new BadRequestException("Token is invalid or expired"));
        AppUser user = users.findByIdForUpdate(preview.getUserId())
                .filter(candidate -> candidate.getTenantId().equals(preview.getTenantId()))
                .orElseThrow(() -> new BadRequestException("Token is invalid or expired"));
        ActionToken token = actionTokens.findByTokenHashAndTypeForUpdate(tokenHash, type)
                .filter(candidate -> candidate.getUserId().equals(user.getId())
                        && candidate.getTenantId().equals(user.getTenantId()))
                .orElseThrow(() -> new BadRequestException("Token is invalid or expired"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now()))
            throw new BadRequestException("Token is invalid or expired");
        return new LockedAction(token, user);
    }

    private Session createSession(AppUser user, Tenant tenant, String userAgent, String ip) {
        return createSession(user, tenant, userAgent, ip, UUID.randomUUID(),
                Instant.now().plus(properties.jwt().refreshTtl()));
    }

    private Session createSession(AppUser user, Tenant tenant, String userAgent, String ip, UUID familyId,
                                  Instant familyExpiresAt) {
        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken token = new RefreshToken();
        token.setTenantId(user.getTenantId());
        token.setUserId(user.getId());
        token.setFamilyId(familyId);
        token.setTokenHash(hash(raw));
        // Rotation is bounded by the original family expiry; refresh sessions cannot slide forever.
        token.setExpiresAt(familyExpiresAt);
        token.setUserAgent(trim(userAgent, 300));
        token.setIpAddress(trim(ip, 64));
        refreshTokens.save(token);
        return new Session(response(user, tenant), raw);
    }

    private AuthResponse response(AppUser user, Tenant tenant) {
        return new AuthResponse(jwtService.createAccessToken(user), jwtService.accessExpiresInSeconds(),
                summary(user, tenant));
    }

    private UserSummary summary(AppUser user, Tenant tenant) {
        return new UserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(),
                tenant.getId(), tenant.getName(), user.getEmailVerifiedAt());
    }

    private String createActionToken(AppUser user, ActionToken.Type type) {
        String raw = opaqueToken();
        ActionToken token = new ActionToken();
        token.setTenantId(user.getTenantId());
        token.setUserId(user.getId());
        token.setType(type);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(properties.jwt().actionTtl()));
        actionTokens.save(token);
        return raw;
    }

    @Transactional(readOnly = true)
    public boolean emailExistsOrPending(String email) {
        String normalized = normalizeEmail(email);
        return users.existsByEmailIgnoreCase(normalized) || pendingRegistrations.existsByEmailIgnoreCase(normalized);
    }

    private static String opaqueToken() { return UUID.randomUUID() + "." + UUID.randomUUID(); }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "company";
        base = base.substring(0, Math.min(base.length(), 65));
        String candidate = base;
        while (tenants.existsBySlug(candidate)) candidate = base + '-' + UUID.randomUUID().toString().substring(0, 8);
        return candidate;
    }

    private static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static String trim(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }
    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    public record Session(AuthResponse response, String refreshToken) {}
    private record LockedAction(ActionToken token, AppUser user) {}
}
