package com.mtcrm.tenant;

import com.mtcrm.activity.ActivityRepository;
import com.mtcrm.auth.PendingRegistrationRepository;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.customer.CustomerRepository;
import com.mtcrm.lead.LeadRepository;
import com.mtcrm.task.TaskRepository;
import com.mtcrm.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantQuotaService {
    public enum Resource { USER, CUSTOMER, LEAD, TASK }

    private final TenantRepository tenants;
    private final DeploymentQuotaLockRepository deploymentGuard;
    private final UserRepository users;
    private final CustomerRepository customers;
    private final LeadRepository leads;
    private final TaskRepository tasks;
    private final ActivityRepository activities;
    private final PendingRegistrationRepository pendingRegistrations;
    private final long maxTenants;
    private final long maxUsers;
    private final long maxCustomers;
    private final long maxLeads;
    private final long maxTasks;
    private final long maxActivities;
    private final long maxPendingRegistrations;

    public TenantQuotaService(TenantRepository tenants, DeploymentQuotaLockRepository deploymentGuard,
                              UserRepository users, CustomerRepository customers, LeadRepository leads,
                              TaskRepository tasks, ActivityRepository activities,
                              PendingRegistrationRepository pendingRegistrations,
                              @Value("${app.quotas.max-tenants:10000}") long maxTenants,
                              @Value("${app.quotas.max-users-per-tenant:250}") long maxUsers,
                              @Value("${app.quotas.max-customers-per-tenant:100000}") long maxCustomers,
                              @Value("${app.quotas.max-leads-per-tenant:100000}") long maxLeads,
                              @Value("${app.quotas.max-tasks-per-tenant:250000}") long maxTasks,
                              @Value("${app.quotas.max-activities-per-tenant:1000000}") long maxActivities,
                              @Value("${app.quotas.max-pending-registrations:5000}") long maxPendingRegistrations) {
        this.tenants = tenants; this.deploymentGuard = deploymentGuard; this.users = users;
        this.customers = customers; this.leads = leads; this.tasks = tasks; this.activities = activities;
        this.pendingRegistrations = pendingRegistrations;
        this.maxTenants = maxTenants; this.maxUsers = maxUsers; this.maxCustomers = maxCustomers;
        this.maxLeads = maxLeads; this.maxTasks = maxTasks; this.maxActivities = maxActivities;
        this.maxPendingRegistrations = maxPendingRegistrations;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assertCanRegisterTenant() {
        deploymentGuard.findByIdForUpdate(1)
                .orElseThrow(() -> new IllegalStateException("Deployment quota guard is missing"));
        if (tenants.count() >= maxTenants) throw new BadRequestException("This deployment is not accepting more workspaces");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assertCanCreatePendingRegistration() {
        deploymentGuard.findByIdForUpdate(1)
                .orElseThrow(() -> new IllegalStateException("Deployment quota guard is missing"));
        if (pendingRegistrations.count() >= maxPendingRegistrations)
            throw new BadRequestException("Too many registrations are awaiting verification; try again later");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assertCanCreate(Resource resource) {
        UUID tenantId = lockCurrentTenant();
        long used = switch (resource) {
            case USER -> users.countByTenantId(tenantId);
            case CUSTOMER -> customers.countByTenantId(tenantId);
            case LEAD -> leads.countByTenantId(tenantId);
            case TASK -> tasks.countByTenantId(tenantId);
        };
        long maximum = switch (resource) {
            case USER -> maxUsers;
            case CUSTOMER -> maxCustomers;
            case LEAD -> maxLeads;
            case TASK -> maxTasks;
        };
        if (used >= maximum) throw new BadRequestException("Workspace " + resource.name().toLowerCase() + " quota has been reached");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void prepareActivitySlot() {
        UUID tenantId = lockCurrentTenant();
        long used = activities.countByTenantId(tenantId);
        if (used < maxActivities) return;
        int remove = (int) Math.min(1000, Math.max(1, used - maxActivities + 1));
        var oldest = activities.findByTenantId(tenantId,
                PageRequest.of(0, remove, Sort.by(Sort.Direction.ASC, "createdAt")));
        activities.deleteAllInBatch(oldest);
    }

    private UUID lockCurrentTenant() {
        UUID tenantId = TenantContext.require();
        tenants.findByIdForUpdate(tenantId).orElseThrow(() -> new NotFoundException("Tenant not found"));
        return tenantId;
    }
}
