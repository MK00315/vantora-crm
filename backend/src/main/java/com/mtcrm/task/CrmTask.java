package com.mtcrm.task;

import com.mtcrm.common.domain.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class CrmTask extends TenantOwnedEntity {
    @Column(nullable = false, length = 180)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status = TaskStatus.TODO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;
    @Column(name = "due_at")
    private Instant dueAt;
    @Column(name = "assignee_id")
    private UUID assigneeId;
    @Column(name = "lead_id")
    private UUID leadId;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;
    @Column(name = "reminder_next_attempt_at")
    private Instant reminderNextAttemptAt;
    @Column(name = "reminder_claimed_at")
    private Instant reminderClaimedAt;
    @Column(name = "reminder_claim_id")
    private UUID reminderClaimId;
    @Column(name = "reminder_attempts", nullable = false)
    private int reminderAttempts;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public UUID getAssigneeId() { return assigneeId; }
    public void setAssigneeId(UUID assigneeId) { this.assigneeId = assigneeId; }
    public UUID getLeadId() { return leadId; }
    public void setLeadId(UUID leadId) { this.leadId = leadId; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(Instant reminderSentAt) { this.reminderSentAt = reminderSentAt; }
    public Instant getReminderNextAttemptAt() { return reminderNextAttemptAt; }
    public void setReminderNextAttemptAt(Instant reminderNextAttemptAt) { this.reminderNextAttemptAt = reminderNextAttemptAt; }
    public Instant getReminderClaimedAt() { return reminderClaimedAt; }
    public void setReminderClaimedAt(Instant reminderClaimedAt) { this.reminderClaimedAt = reminderClaimedAt; }
    public UUID getReminderClaimId() { return reminderClaimId; }
    public void setReminderClaimId(UUID reminderClaimId) { this.reminderClaimId = reminderClaimId; }
    public int getReminderAttempts() { return reminderAttempts; }
    public void setReminderAttempts(int reminderAttempts) { this.reminderAttempts = reminderAttempts; }

    public void resetReminderDelivery() {
        reminderSentAt = null;
        reminderNextAttemptAt = null;
        reminderClaimedAt = null;
        reminderClaimId = null;
        reminderAttempts = 0;
    }
}
