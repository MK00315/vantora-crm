package com.mtcrm.notification;

import com.mtcrm.task.CrmTask;
import com.mtcrm.task.TaskRepository;
import com.mtcrm.task.TaskStatus;
import com.mtcrm.tenant.Tenant;
import com.mtcrm.tenant.TenantRepository;
import com.mtcrm.user.UserStatus;
import com.mtcrm.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TaskReminderQueue {
    private static final List<TaskStatus> DONE = List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED);
    private static final Duration STALE_CLAIM = Duration.ofMinutes(35);
    private final TaskRepository tasks;
    private final UserRepository users;
    private final TenantRepository tenants;

    public TaskReminderQueue(TaskRepository tasks, UserRepository users, TenantRepository tenants) {
        this.tasks = tasks;
        this.users = users;
        this.tenants = tenants;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Delivery> claimBatch(Instant now) {
        List<UUID> tenantIds = tasks.findReminderTenantIds(now, now.minus(STALE_CLAIM), DONE,
                PageRequest.of(0, 50));
        List<Delivery> deliveries = new ArrayList<>(tenantIds.size());
        for (UUID tenantId : tenantIds) {
            Tenant tenant = tenants.findByIdForUpdate(tenantId).filter(Tenant::isActive).orElse(null);
            if (tenant == null) continue;
            List<CrmTask> candidates = tasks.findReminderCandidateForTenant(tenantId, now, now.minus(STALE_CLAIM),
                    DONE, PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "dueAt")));
            if (candidates.isEmpty()) continue;
            CrmTask task = candidates.getFirst();
            UUID claimId = UUID.randomUUID();
            task.setReminderClaimId(claimId);
            task.setReminderClaimedAt(now);
            tenant.setReminderLastServedAt(now);
            deliveries.add(new Delivery(task.getId(), task.getTenantId(), claimId));
        }
        return deliveries;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public java.util.Optional<PreparedDelivery> prepareDelivery(Delivery delivery, Instant now) {
        CrmTask task = tasks.findByIdAndTenantIdForUpdate(delivery.taskId(), delivery.tenantId()).orElse(null);
        if (task == null || !delivery.claimId().equals(task.getReminderClaimId())) return java.util.Optional.empty();
        boolean taskEligible = task.getReminderSentAt() == null && task.getDueAt() != null
                && task.getDueAt().isBefore(now) && !DONE.contains(task.getStatus());
        var user = taskEligible && task.getAssigneeId() != null
                ? users.findByIdAndTenantId(task.getAssigneeId(), task.getTenantId()).orElse(null) : null;
        boolean recipientEligible = user != null && user.getStatus() == UserStatus.ACTIVE
                && user.getEmailVerifiedAt() != null;
        boolean tenantActive = tenants.findById(task.getTenantId()).filter(Tenant::isActive).isPresent();
        if (!taskEligible || !recipientEligible || !tenantActive) {
            releaseWithBackoff(task, now);
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new PreparedDelivery(user.getEmail(), task.getTitle()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Delivery delivery, Instant sentAt) {
        tasks.findByIdAndTenantIdForUpdate(delivery.taskId(), delivery.tenantId())
                .filter(task -> delivery.claimId().equals(task.getReminderClaimId()))
                .ifPresent(task -> {
                    task.setReminderSentAt(sentAt);
                    task.setReminderClaimId(null);
                    task.setReminderClaimedAt(null);
                    task.setReminderNextAttemptAt(null);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Delivery delivery, Instant failedAt) {
        tasks.findByIdAndTenantIdForUpdate(delivery.taskId(), delivery.tenantId())
                .filter(task -> delivery.claimId().equals(task.getReminderClaimId()))
                .ifPresent(task -> releaseWithBackoff(task, failedAt));
    }

    private static void releaseWithBackoff(CrmTask task, Instant failedAt) {
        int attempts = Math.min(task.getReminderAttempts() + 1, 1000);
        long hours = Math.min(24, 1L << Math.min(Math.max(0, attempts - 1), 4));
        task.setReminderAttempts(attempts);
        task.setReminderClaimId(null);
        task.setReminderClaimedAt(null);
        task.setReminderNextAttemptAt(failedAt.plus(Duration.ofHours(hours)));
    }

    public record Delivery(UUID taskId, UUID tenantId, UUID claimId) {}
    public record PreparedDelivery(String email, String title) {}
}
