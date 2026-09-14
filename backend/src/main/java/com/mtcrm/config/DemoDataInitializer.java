package com.mtcrm.config;

import com.mtcrm.customer.Customer;
import com.mtcrm.customer.CustomerRepository;
import com.mtcrm.customer.CustomerStatus;
import com.mtcrm.lead.Lead;
import com.mtcrm.lead.LeadRepository;
import com.mtcrm.lead.LeadStage;
import com.mtcrm.task.CrmTask;
import com.mtcrm.task.TaskPriority;
import com.mtcrm.task.TaskRepository;
import com.mtcrm.task.TaskStatus;
import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.user.AppUser;
import com.mtcrm.user.Role;
import com.mtcrm.user.UserRepository;
import com.mtcrm.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@Profile("dev")
public class DemoDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private final AppProperties properties;
    private final TenantRepository tenants;
    private final UserRepository users;
    private final CustomerRepository customers;
    private final LeadRepository leads;
    private final TaskRepository tasks;
    private final PasswordEncoder passwords;

    public DemoDataInitializer(AppProperties properties, TenantRepository tenants, UserRepository users,
                               CustomerRepository customers, LeadRepository leads, TaskRepository tasks,
                               PasswordEncoder passwords) {
        this.properties = properties; this.tenants = tenants; this.users = users; this.customers = customers;
        this.leads = leads; this.tasks = tasks; this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.demo().enabled()) return;
        if (properties.demo().password() == null || properties.demo().password().isBlank()) {
            log.warn("Demo seed requested but DEMO_PASSWORD is empty; no demo account was created");
            return;
        }
        if (tenants.existsBySlug("acme-demo")) return;

        Tenant tenant = new Tenant(); tenant.setName("Acme Demo"); tenant.setSlug("acme-demo"); tenants.save(tenant);
        AppUser admin = new AppUser(); admin.setTenantId(tenant.getId()); admin.setFirstName("Alex"); admin.setLastName("Morgan");
        admin.setEmail("admin@acme.test"); admin.setPasswordHash(passwords.encode(properties.demo().password()));
        admin.setRole(Role.COMPANY_ADMIN); admin.setStatus(UserStatus.ACTIVE); admin.setEmailVerifiedAt(Instant.now()); users.save(admin);

        Customer customer = new Customer(); customer.setTenantId(tenant.getId()); customer.setName("Vantora Labs");
        customer.setCompany("Vantora Labs"); customer.setEmail("hello@vantora.test"); customer.setStatus(CustomerStatus.ACTIVE);
        customer.setOwnerId(admin.getId()); customers.save(customer);

        Lead lead = new Lead(); lead.setTenantId(tenant.getId()); lead.setFirstName("Jordan"); lead.setLastName("Lee");
        lead.setEmail("jordan@example.test"); lead.setCompany("Brightworks"); lead.setSource("Referral");
        lead.setStage(LeadStage.QUALIFIED); lead.setEstimatedValue(new BigDecimal("18000.00")); lead.setOwnerId(admin.getId()); leads.save(lead);

        CrmTask task = new CrmTask(); task.setTenantId(tenant.getId()); task.setTitle("Prepare discovery call");
        task.setStatus(TaskStatus.TODO); task.setPriority(TaskPriority.HIGH); task.setDueAt(Instant.now().plusSeconds(86400));
        task.setAssigneeId(admin.getId()); task.setLeadId(lead.getId()); tasks.save(task);
        log.info("Demo tenant created for admin@acme.test; password supplied by DEMO_PASSWORD");
    }
}
