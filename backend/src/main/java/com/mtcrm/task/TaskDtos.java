package com.mtcrm.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class TaskDtos {
    private TaskDtos() {}
    public record Request(
            @NotBlank @Size(max = 180) String title,
            @Size(max = 5000) String description,
            @NotNull TaskStatus status,
            @NotNull TaskPriority priority,
            @com.fasterxml.jackson.annotation.JsonAlias("dueDate") Instant dueAt,
            UUID assigneeId,
            UUID leadId
    ) {}
    public record StatusRequest(@NotNull TaskStatus status) {}
    public record Response(UUID id, String title, String description, TaskStatus status, TaskPriority priority,
                           Instant dueAt, UUID assigneeId, UUID leadId, Instant completedAt,
                           Instant createdAt, Instant updatedAt) {
        public static Response from(CrmTask t) {
            return new Response(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(),
                    t.getDueAt(), t.getAssigneeId(), t.getLeadId(), t.getCompletedAt(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }
}
