package com.mtcrm.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<CrmTask, UUID> {
    Optional<CrmTask> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantId(UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from CrmTask task where task.id = :id and task.tenantId = :tenantId")
    Optional<CrmTask> findByIdAndTenantIdForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    long countByTenantIdAndStatusNotIn(UUID tenantId, List<TaskStatus> statuses);
    long countByTenantIdAndDueAtBeforeAndStatusNotIn(UUID tenantId, Instant now, List<TaskStatus> statuses);
    List<CrmTask> findTop5ByTenantIdAndDueAtIsNotNullAndStatusNotInOrderByDueAtAsc(UUID tenantId, List<TaskStatus> statuses);

    @Query("select t.status, count(t) from CrmTask t where t.tenantId = :tenantId group by t.status")
    List<Object[]> statusSummary(@Param("tenantId") UUID tenantId);

    @Query("""
        select t from CrmTask t where t.tenantId = :tenantId
          and (:q = '' or lower(t.title) like lower(concat('%', :q, '%')))
          and (:status is null or t.status = :status)
          and (:priority is null or t.priority = :priority)
          and (:assigneeId is null or t.assigneeId = :assigneeId)
        """)
    Page<CrmTask> search(@Param("tenantId") UUID tenantId, @Param("q") String query,
                         @Param("status") TaskStatus status, @Param("priority") TaskPriority priority,
                         @Param("assigneeId") UUID assigneeId, Pageable pageable);

    @Query("""
        select task.tenantId from CrmTask task, AppUser assignee, Tenant tenant
        where task.dueAt < :now and task.status not in :done and task.reminderSentAt is null
          and (task.reminderNextAttemptAt is null or task.reminderNextAttemptAt <= :now)
          and (task.reminderClaimedAt is null or task.reminderClaimedAt < :staleBefore)
          and assignee.id = task.assigneeId and assignee.tenantId = task.tenantId
          and assignee.status = com.mtcrm.user.UserStatus.ACTIVE and assignee.emailVerifiedAt is not null
          and tenant.id = task.tenantId and tenant.active = true
        group by task.tenantId, tenant.reminderLastServedAt
        order by case when tenant.reminderLastServedAt is null then 0 else 1 end,
          tenant.reminderLastServedAt asc, min(task.dueAt) asc
        """)
    List<UUID> findReminderTenantIds(@Param("now") Instant now, @Param("staleBefore") Instant staleBefore,
                                     @Param("done") List<TaskStatus> done, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select task from CrmTask task, AppUser assignee, Tenant tenant
        where task.tenantId = :tenantId and task.dueAt < :now and task.status not in :done
          and task.reminderSentAt is null
          and (task.reminderNextAttemptAt is null or task.reminderNextAttemptAt <= :now)
          and (task.reminderClaimedAt is null or task.reminderClaimedAt < :staleBefore)
          and assignee.id = task.assigneeId and assignee.tenantId = task.tenantId
          and assignee.status = com.mtcrm.user.UserStatus.ACTIVE and assignee.emailVerifiedAt is not null
          and tenant.id = task.tenantId and tenant.active = true
        """)
    List<CrmTask> findReminderCandidateForTenant(@Param("tenantId") UUID tenantId,
                                                  @Param("now") Instant now,
                                                  @Param("staleBefore") Instant staleBefore,
                                                  @Param("done") List<TaskStatus> done,
                                                  Pageable pageable);
}
