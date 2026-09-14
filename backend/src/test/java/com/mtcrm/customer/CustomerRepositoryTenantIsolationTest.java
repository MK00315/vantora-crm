package com.mtcrm.customer;

import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTenantIsolationTest {
    @Autowired TenantRepository tenants;
    @Autowired CustomerRepository customers;

    @Test
    void sameIdCannotBeReadThroughAnotherTenantScope() {
        Tenant first = tenant("First", "first-" + UUID.randomUUID());
        Tenant second = tenant("Second", "second-" + UUID.randomUUID());
        Customer customer = new Customer(); customer.setTenantId(first.getId()); customer.setName("Private customer");
        customer.setStatus(CustomerStatus.ACTIVE); customers.saveAndFlush(customer);

        assertThat(customers.findByIdAndTenantId(customer.getId(), first.getId())).isPresent();
        assertThat(customers.findByIdAndTenantId(customer.getId(), second.getId())).isEmpty();
    }

    private Tenant tenant(String name, String slug) {
        Tenant tenant = new Tenant(); tenant.setName(name); tenant.setSlug(slug); return tenants.saveAndFlush(tenant);
    }
}
