package com.mtcrm.customer;

import com.mtcrm.activity.ActivityService;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantQuotaService;
import com.mtcrm.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock CustomerRepository customers;
    @Mock UserRepository users;
    @Mock ActivityService activities;
    @Mock TenantQuotaService quotas;

    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test
    void rejectsOwnerThatDoesNotBelongToAuthenticatedTenant() {
        UUID tenant = UUID.randomUUID();
        UUID foreignOwner = UUID.randomUUID();
        TenantContext.set(tenant);
        when(users.findByIdAndTenantId(foreignOwner, tenant)).thenReturn(Optional.empty());
        var service = new CustomerService(customers, users, activities, quotas);
        var request = new CustomerDtos.Request("Acme", "a@acme.test", null, "Acme", null,
                CustomerStatus.ACTIVE, null, foreignOwner);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your company");
        verify(customers, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void lookupAlwaysIncludesAuthenticatedTenant() {
        UUID tenant = UUID.randomUUID(); UUID customer = UUID.randomUUID();
        TenantContext.set(tenant);
        when(customers.findByIdAndTenantId(customer, tenant)).thenReturn(Optional.empty());
        var service = new CustomerService(customers, users, activities, quotas);

        assertThatThrownBy(() -> service.get(customer)).isInstanceOf(com.mtcrm.common.exception.NotFoundException.class);
        verify(customers).findByIdAndTenantId(customer, tenant);
    }
}
