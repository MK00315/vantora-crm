package com.mtcrm.notification;

import com.mtcrm.task.CrmTask;
import com.mtcrm.task.TaskRepository;
import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskReminderQueueTest {
    @Test
    void failedClaimUsesDurableBackoffAndDoesNotTouchAReplacedClaim() {
        TaskRepository tasks = mock(TaskRepository.class);
        UserRepository users = mock(UserRepository.class);
        UUID taskId = UUID.randomUUID(), tenantId = UUID.randomUUID(), claimId = UUID.randomUUID();
        CrmTask task = new CrmTask();
        task.setId(taskId); task.setTenantId(tenantId); task.setReminderClaimId(claimId);
        when(tasks.findByIdAndTenantIdForUpdate(taskId, tenantId)).thenReturn(Optional.of(task));
        var queue = new TaskReminderQueue(tasks, users, mock(TenantRepository.class));
        Instant failedAt = Instant.parse("2026-08-24T00:00:00Z");

        queue.markFailed(new TaskReminderQueue.Delivery(taskId, tenantId, claimId), failedAt);

        assertThat(task.getReminderAttempts()).isEqualTo(1);
        assertThat(task.getReminderNextAttemptAt()).isEqualTo(failedAt.plusSeconds(3600));
        assertThat(task.getReminderClaimId()).isNull();
    }

    @Test
    void claimingATaskPersistsTheTenantsFairnessCursor() {
        TaskRepository tasks = mock(TaskRepository.class);
        UserRepository users = mock(UserRepository.class);
        TenantRepository tenants = mock(TenantRepository.class);
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant(); tenant.setId(tenantId); tenant.setName("Tenant"); tenant.setSlug("tenant");
        CrmTask task = new CrmTask(); task.setId(UUID.randomUUID()); task.setTenantId(tenantId);
        Instant now = Instant.parse("2026-08-24T12:00:00Z");
        when(tasks.findReminderTenantIds(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(tenantId));
        when(tenants.findByIdForUpdate(tenantId)).thenReturn(Optional.of(tenant));
        when(tasks.findReminderCandidateForTenant(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.List.of(task));

        var claimed = new TaskReminderQueue(tasks, users, tenants).claimBatch(now);

        assertThat(claimed).hasSize(1);
        assertThat(tenant.getReminderLastServedAt()).isEqualTo(now);
    }
}
