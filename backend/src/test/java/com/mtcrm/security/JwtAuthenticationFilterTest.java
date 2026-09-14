package com.mtcrm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.user.AppUser;
import com.mtcrm.user.Role;
import com.mtcrm.user.UserRepository;
import com.mtcrm.user.UserStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock JwtService jwtService;
    @Mock UserRepository users;
    @Mock TenantRepository tenants;
    @Mock FilterChain chain;

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void authenticatesAsyncDispatchesUsedByStreamingExports() {
        var filter = new JwtAuthenticationFilter(jwtService, users, tenants,
                new ObjectMapper().findAndRegisterModules());

        assertThat(filter.shouldNotFilterAsyncDispatch()).isFalse();
    }

    @Test
    void rejectsAStaleTokenAfterTheDatabaseRoleChanges() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        var tokenPrincipal = new CurrentUser(userId, tenantId, "user@example.test", Role.COMPANY_ADMIN, 0);
        AppUser user = new AppUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setEmail("user@example.test");
        user.setRole(Role.EMPLOYEE);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(java.time.Instant.now());
        Tenant active = new Tenant();
        active.setId(tenantId);
        active.setName("Active tenant");
        active.setSlug("active-tenant");
        when(jwtService.parseAccessToken("access-token")).thenReturn(tokenPrincipal);
        when(users.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(tenants.findById(tenantId)).thenReturn(Optional.of(active));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        var response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(jwtService, users, tenants, new ObjectMapper().findAndRegisterModules())
                .doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_TOKEN");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(TenantContext.get()).isEmpty();
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsAnOtherwiseValidTokenWhenTheTenantWasDeactivated() throws Exception {
        UUID userId = UUID.randomUUID(), tenantId = UUID.randomUUID();
        var tokenPrincipal = new CurrentUser(userId, tenantId, "user@example.test", Role.COMPANY_ADMIN, 0);
        AppUser user = new AppUser(); user.setId(userId); user.setTenantId(tenantId); user.setEmail("user@example.test");
        user.setRole(Role.COMPANY_ADMIN); user.setStatus(UserStatus.ACTIVE); user.setEmailVerifiedAt(java.time.Instant.now());
        Tenant inactive = new Tenant(); inactive.setId(tenantId); inactive.setName("Inactive"); inactive.setSlug("inactive");
        inactive.setActive(false);
        when(jwtService.parseAccessToken("access-token")).thenReturn(tokenPrincipal);
        when(users.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(tenants.findById(tenantId)).thenReturn(Optional.of(inactive));

        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer access-token");
        var response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(jwtService, users, tenants, new ObjectMapper().findAndRegisterModules())
                .doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }
}
