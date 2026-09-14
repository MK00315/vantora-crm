package com.mtcrm.notification;

import com.mtcrm.task.CrmTask;
import com.mtcrm.task.TaskRepository;
import com.mtcrm.task.TaskStatus;
import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.user.AppUser;
import com.mtcrm.user.Role;
import com.mtcrm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TaskReminderFairnessRepositoryTest {
    @Autowired TenantRepository tenants;
    @Autowired UserRepository users;
    @Autowired TaskRepository tasks;

    @Test
    void anUnservedTenantEntersTheNextBatchWhenMoreThanFiftyHaveBacklogs() {
        Instant now = Instant.parse("2026-08-24T12:00:00Z");
        List<UUID> olderBacklogTenants = new ArrayList<>();
        UUID waitingTenant = null;
        for (int index = 0; index < 51; index++) {
            Tenant tenant = new Tenant(); tenant.setName("Tenant " + index);
            tenant.setSlug("fair-" + index + '-' + UUID.randomUUID());
            tenant = tenants.saveAndFlush(tenant);

            AppUser user = new AppUser(); user.setTenantId(tenant.getId()); user.setFirstName("User");
            user.setLastName(String.valueOf(index)); user.setEmail("fair-" + index + '-' + UUID.randomUUID() + "@example.test");
            user.setPasswordHash("not-used-in-this-repository-test"); user.setRole(Role.EMPLOYEE);
            user.setEmailVerifiedAt(now.minusSeconds(86_400));
            user = users.saveAndFlush(user);

            CrmTask task = new CrmTask(); task.setTenantId(tenant.getId()); task.setAssigneeId(user.getId());
            task.setTitle("Reminder " + index);
            task.setDueAt(index < 50 ? now.minusSeconds(172_800) : now.minusSeconds(86_400));
            tasks.save(task);
            if (index < 50) olderBacklogTenants.add(tenant.getId()); else waitingTenant = tenant.getId();
        }
        tasks.flush();

        List<UUID> first = eligibleTenants(now);
        assertThat(first).hasSize(50).containsExactlyInAnyOrderElementsOf(olderBacklogTenants);
        first.forEach(id -> tenants.findById(id).orElseThrow().setReminderLastServedAt(now));
        tenants.flush();

        assertThat(eligibleTenants(now)).hasSize(50).contains(waitingTenant);
    }

    private List<UUID> eligibleTenants(Instant now) {
        return tasks.findReminderTenantIds(now, now.minusSeconds(2_100),
                List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED), PageRequest.of(0, 50));
    }
}
