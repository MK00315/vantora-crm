package com.mtcrm.user;

import com.mtcrm.activity.ActivityService;
import com.mtcrm.auth.AuthService;
import com.mtcrm.common.api.PageResponse;
import com.mtcrm.common.api.PageableFactory;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.common.exception.ConflictException;
import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantQuotaService;
import com.mtcrm.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    private static final Set<String> SORTS = Set.of("firstName", "lastName", "email", "role", "status", "createdAt");
    private final UserRepository users;
    private final TenantRepository tenants;
    private final PasswordEncoder passwords;
    private final AuthService auth;
    private final ActivityService activities;
    private final TenantQuotaService quotas;

    public UserService(UserRepository users, TenantRepository tenants, PasswordEncoder passwords,
                       AuthService auth, ActivityService activities, TenantQuotaService quotas) {
        this.users = users; this.tenants = tenants; this.passwords = passwords; this.auth = auth;
        this.activities = activities; this.quotas = quotas;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDtos.Response> list(String query, Role role, UserStatus status, int page, int size,
                                                String sort, String direction) {
        var result = users.search(TenantContext.require(), query == null ? "" : query.trim(), role, status,
                PageableFactory.create(page, size, sort, direction, SORTS));
        return PageResponse.from(result, UserDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public UserDtos.Response get(UUID id) { return UserDtos.Response.from(find(id)); }

    @Transactional
    public UserDtos.Response invite(CurrentUser actor, UserDtos.InviteRequest request) {
        quotas.assertCanCreate(TenantQuotaService.Resource.USER);
        validateRole(request.role(), actor.role());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (auth.emailExistsOrPending(email)) throw new ConflictException("An account with this email already exists");
        AppUser user = new AppUser();
        user.setTenantId(TenantContext.require());
        user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim()); user.setEmail(email);
        user.setRole(request.role()); user.setStatus(UserStatus.INVITED);
        user.setPasswordHash(passwords.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
        users.save(user);
        auth.forgotPassword(email);
        activities.record("INVITED", "USER", user.getId(), "Invited " + user.fullName());
        return UserDtos.Response.from(user);
    }

    @Transactional
    public UserDtos.Response update(UUID id, CurrentUser actor, UserDtos.UpdateRequest request) {
        validateRole(request.role(), actor.role());
        lockTenant();
        AppUser user = findForUpdate(id);
        guardPlatformAdministrator(user, actor);
        if (id.equals(actor.id()) && request.status() != UserStatus.ACTIVE)
            throw new BadRequestException("You cannot suspend your own account");
        boolean removesActiveAdmin = user.getRole() == Role.COMPANY_ADMIN && user.getStatus() == UserStatus.ACTIVE
                && (request.role() != Role.COMPANY_ADMIN || request.status() != UserStatus.ACTIVE);
        if (removesActiveAdmin
                && users.countByTenantIdAndRoleAndStatus(TenantContext.require(), Role.COMPANY_ADMIN, UserStatus.ACTIVE) <= 1)
            throw new BadRequestException("At least one active company administrator is required");
        boolean authorizationChanged = user.getRole() != request.role() || user.getStatus() != request.status();
        user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setRole(request.role()); user.setStatus(request.status());
        if (authorizationChanged) auth.revokeSessions(id);
        activities.record("UPDATED", "USER", id, "Updated team member " + user.fullName());
        return UserDtos.Response.from(user);
    }

    @Transactional
    public UserDtos.Response updateProfile(UUID userId, UserDtos.ProfileRequest request) {
        AppUser user = find(userId);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        activities.record("UPDATED", "PROFILE", userId, "Updated personal profile");
        return UserDtos.Response.from(user);
    }

    @Transactional
    public void changePassword(UUID userId, UserDtos.PasswordRequest request) {
        lockTenant();
        AppUser user = findForUpdate(userId);
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash()))
            throw new BadRequestException("Current password is incorrect");
        if (passwords.matches(request.newPassword(), user.getPasswordHash()))
            throw new BadRequestException("New password must be different from the current password");
        user.setPasswordHash(passwords.encode(request.newPassword()));
        auth.revokeSessions(userId);
        auth.invalidatePasswordResets(userId);
        activities.record("PASSWORD_CHANGED", "PROFILE", userId, "Changed account password");
    }

    @Transactional
    public void deactivate(UUID id, CurrentUser actor) {
        if (id.equals(actor.id())) throw new BadRequestException("You cannot deactivate your own account");
        lockTenant();
        AppUser user = findForUpdate(id);
        guardPlatformAdministrator(user, actor);
        if (user.getRole() == Role.COMPANY_ADMIN && user.getStatus() == UserStatus.ACTIVE
                && users.countByTenantIdAndRoleAndStatus(TenantContext.require(), Role.COMPANY_ADMIN, UserStatus.ACTIVE) <= 1)
            throw new BadRequestException("At least one active company administrator is required");
        user.setStatus(UserStatus.SUSPENDED);
        auth.revokeSessions(id);
        activities.record("DEACTIVATED", "USER", id, "Deactivated team member " + user.fullName());
    }

    private AppUser find(UUID id) {
        return users.findByIdAndTenantId(id, TenantContext.require()).orElseThrow(() -> new NotFoundException("User not found"));
    }
    private AppUser findForUpdate(UUID id) {
        return users.findByIdAndTenantIdForUpdate(id, TenantContext.require())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
    private void lockTenant() {
        UUID tenantId = TenantContext.require();
        tenants.findByIdForUpdate(tenantId).orElseThrow(() -> new NotFoundException("Tenant not found"));
    }
    private static void validateRole(Role role, Role actorRole) {
        if (role == Role.SUPER_ADMIN && actorRole != Role.SUPER_ADMIN)
            throw new AccessDeniedException("Only a platform administrator can manage this role");
    }
    private static void guardPlatformAdministrator(AppUser target, CurrentUser actor) {
        if (target.getRole() == Role.SUPER_ADMIN && actor.role() != Role.SUPER_ADMIN)
            throw new AccessDeniedException("Only a platform administrator can manage this account");
    }
}
