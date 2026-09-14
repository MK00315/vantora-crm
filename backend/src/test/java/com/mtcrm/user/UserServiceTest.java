package com.mtcrm.user;

import com.mtcrm.activity.ActivityService;
import com.mtcrm.auth.AuthService;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.tenant.TenantQuotaService;
import com.mtcrm.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock TenantRepository tenants;
    @Mock PasswordEncoder passwords;
    @Mock AuthService auth;
    @Mock ActivityService activities;
    @Mock TenantQuotaService quotas;
    @AfterEach void clear() { TenantContext.clear(); }

    @Test
    void refusesToDeactivateLastActiveCompanyAdmin() {
        UUID tenant = UUID.randomUUID(), actor = UUID.randomUUID(), target = UUID.randomUUID();
        TenantContext.set(tenant);
        when(tenants.findByIdForUpdate(tenant)).thenReturn(Optional.of(new Tenant()));
        AppUser admin = new AppUser(); admin.setId(target); admin.setTenantId(tenant);
        admin.setFirstName("Only"); admin.setLastName("Admin"); admin.setRole(Role.COMPANY_ADMIN); admin.setStatus(UserStatus.ACTIVE);
        when(users.findByIdAndTenantIdForUpdate(target, tenant)).thenReturn(Optional.of(admin));
        when(users.countByTenantIdAndRoleAndStatus(tenant, Role.COMPANY_ADMIN, UserStatus.ACTIVE)).thenReturn(1L);
        var service = new UserService(users, tenants, passwords, auth, activities, quotas);

        assertThatThrownBy(() -> service.deactivate(target, current(actor))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one");
        verify(auth, never()).revokeSessions(target);
    }

    @Test
    void refusesToDemoteLastActiveCompanyAdmin() {
        UUID tenant = UUID.randomUUID(), actor = UUID.randomUUID(), target = UUID.randomUUID();
        TenantContext.set(tenant);
        when(tenants.findByIdForUpdate(tenant)).thenReturn(Optional.of(new Tenant()));
        AppUser admin = new AppUser(); admin.setId(target); admin.setTenantId(tenant);
        admin.setFirstName("Only"); admin.setLastName("Admin"); admin.setRole(Role.COMPANY_ADMIN); admin.setStatus(UserStatus.ACTIVE);
        when(users.findByIdAndTenantIdForUpdate(target, tenant)).thenReturn(Optional.of(admin));
        when(users.countByTenantIdAndRoleAndStatus(tenant, Role.COMPANY_ADMIN, UserStatus.ACTIVE)).thenReturn(1L);
        var service = new UserService(users, tenants, passwords, auth, activities, quotas);

        var request = new UserDtos.UpdateRequest("Only", "Admin", Role.RECRUITER, UserStatus.ACTIVE);
        assertThatThrownBy(() -> service.update(target, current(actor), request)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one");
        verify(auth, never()).revokeSessions(target);
    }

    @Test
    void revokesRefreshSessionsWhenRoleChanges() {
        UUID tenant = UUID.randomUUID(), actor = UUID.randomUUID(), target = UUID.randomUUID();
        TenantContext.set(tenant);
        when(tenants.findByIdForUpdate(tenant)).thenReturn(Optional.of(new Tenant()));
        AppUser user = new AppUser(); user.setId(target); user.setTenantId(tenant);
        user.setFirstName("Ravi"); user.setLastName("Recruiter"); user.setRole(Role.RECRUITER); user.setStatus(UserStatus.ACTIVE);
        when(users.findByIdAndTenantIdForUpdate(target, tenant)).thenReturn(Optional.of(user));
        var service = new UserService(users, tenants, passwords, auth, activities, quotas);

        service.update(target, current(actor), new UserDtos.UpdateRequest("Ravi", "Recruiter", Role.EMPLOYEE, UserStatus.ACTIVE));

        verify(auth).revokeSessions(target);
        verify(tenants).findByIdForUpdate(tenant);
    }

    @Test
    void passwordChangeRevokesSessionsAndOutstandingRecoveryLinks() {
        UUID tenant = UUID.randomUUID(), userId = UUID.randomUUID();
        TenantContext.set(tenant);
        when(tenants.findByIdForUpdate(tenant)).thenReturn(Optional.of(new Tenant()));
        AppUser user = new AppUser(); user.setId(userId); user.setTenantId(tenant); user.setFirstName("Alex");
        user.setLastName("Morgan"); user.setPasswordHash("old-hash");
        when(users.findByIdAndTenantIdForUpdate(userId, tenant)).thenReturn(Optional.of(user));
        when(passwords.matches("Current123", "old-hash")).thenReturn(true);
        when(passwords.matches("NewPassword123", "old-hash")).thenReturn(false);
        when(passwords.encode("NewPassword123")).thenReturn("new-hash");
        var service = new UserService(users, tenants, passwords, auth, activities, quotas);

        service.changePassword(userId, new UserDtos.PasswordRequest("Current123", "NewPassword123"));

        verify(auth).revokeSessions(userId);
        verify(auth).invalidatePasswordResets(userId);
    }

    private static CurrentUser current(UUID id) {
        return new CurrentUser(id, TenantContext.require(), "admin@example.test", Role.COMPANY_ADMIN, 0);
    }
}
