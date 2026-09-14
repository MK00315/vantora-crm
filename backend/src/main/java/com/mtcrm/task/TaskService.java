package com.mtcrm.task;

import com.mtcrm.activity.ActivityService;
import com.mtcrm.common.api.PageResponse;
import com.mtcrm.common.api.PageableFactory;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.lead.LeadRepository;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantQuotaService;
import com.mtcrm.user.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {
    private static final Set<String> SORTS = Set.of("title", "status", "priority", "dueAt", "createdAt", "updatedAt");
    private final TaskRepository tasks;
    private final UserRepository users;
    private final LeadRepository leads;
    private final ActivityService activities;
    private final TenantQuotaService quotas;

    public TaskService(TaskRepository tasks, UserRepository users, LeadRepository leads, ActivityService activities,
                       TenantQuotaService quotas) {
        this.tasks = tasks; this.users = users; this.leads = leads; this.activities = activities; this.quotas = quotas;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskDtos.Response> list(String query, TaskStatus status, TaskPriority priority, UUID assigneeId,
                                                int page, int size, String sort, String direction) {
        if (sort != null && sort.startsWith("dueDate")) sort = "dueAt" + sort.substring("dueDate".length());
        var result = tasks.search(TenantContext.require(), query == null ? "" : query.trim(), status, priority, assigneeId,
                PageableFactory.create(page, size, sort, direction, SORTS));
        return PageResponse.from(result, TaskDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public TaskDtos.Response get(UUID id) { return TaskDtos.Response.from(find(id)); }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public TaskDtos.Response create(TaskDtos.Request request) {
        quotas.assertCanCreate(TenantQuotaService.Resource.TASK);
        validateReferences(request.assigneeId(), request.leadId());
        CrmTask task = new CrmTask();
        task.setTenantId(TenantContext.require());
        apply(task, request);
        tasks.save(task);
        activities.record("CREATED", "TASK", task.getId(), "Created task " + task.getTitle());
        return TaskDtos.Response.from(task);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public TaskDtos.Response update(UUID id, TaskDtos.Request request) {
        validateReferences(request.assigneeId(), request.leadId());
        CrmTask task = find(id);
        apply(task, request);
        activities.record("UPDATED", "TASK", id, "Updated task " + task.getTitle());
        return TaskDtos.Response.from(task);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public TaskDtos.Response changeStatus(UUID id, TaskStatus status) {
        CrmTask task = find(id);
        if (task.getStatus() != status) task.resetReminderDelivery();
        task.setStatus(status);
        task.setCompletedAt(status == TaskStatus.COMPLETED ? Instant.now() : null);
        activities.record("STATUS_CHANGED", "TASK", id, "Changed task status to " + status);
        return TaskDtos.Response.from(task);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public void delete(UUID id) {
        CrmTask task = find(id);
        tasks.delete(task);
        activities.record("DELETED", "TASK", id, "Deleted task " + task.getTitle());
    }

    private CrmTask find(UUID id) {
        return tasks.findByIdAndTenantId(id, TenantContext.require()).orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private void validateReferences(UUID assignee, UUID lead) {
        UUID tenant = TenantContext.require();
        if (assignee != null && users.findByIdAndTenantId(assignee, tenant).isEmpty())
            throw new BadRequestException("Assignee must be a user in your company");
        if (lead != null && leads.findByIdAndTenantId(lead, tenant).isEmpty())
            throw new BadRequestException("Lead must belong to your company");
    }

    private static void apply(CrmTask t, TaskDtos.Request r) {
        boolean reminderChanged = !Objects.equals(t.getDueAt(), r.dueAt())
                || !Objects.equals(t.getAssigneeId(), r.assigneeId()) || t.getStatus() != r.status();
        if (reminderChanged) t.resetReminderDelivery();
        t.setTitle(r.title().trim());
        t.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        t.setStatus(r.status()); t.setPriority(r.priority()); t.setDueAt(r.dueAt());
        t.setAssigneeId(r.assigneeId()); t.setLeadId(r.leadId());
        t.setCompletedAt(r.status() == TaskStatus.COMPLETED ? (t.getCompletedAt() == null ? Instant.now() : t.getCompletedAt()) : null);
    }
}
