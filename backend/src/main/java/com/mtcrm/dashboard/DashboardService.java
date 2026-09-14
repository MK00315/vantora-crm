package com.mtcrm.dashboard;

import com.mtcrm.activity.ActivityRepository;
import com.mtcrm.activity.ActivityService;
import com.mtcrm.customer.CustomerRepository;
import com.mtcrm.customer.CustomerStatus;
import com.mtcrm.lead.LeadRepository;
import com.mtcrm.lead.LeadStage;
import com.mtcrm.task.TaskPriority;
import com.mtcrm.task.TaskRepository;
import com.mtcrm.task.TaskStatus;
import com.mtcrm.tenant.TenantContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private static final List<TaskStatus> DONE = List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED);
    private final LeadRepository leads;
    private final CustomerRepository customers;
    private final TaskRepository tasks;
    private final ActivityRepository activities;

    public DashboardService(LeadRepository leads, CustomerRepository customers, TaskRepository tasks,
                            ActivityRepository activities) {
        this.leads = leads; this.customers = customers; this.tasks = tasks; this.activities = activities;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public Response get() {
        UUID tenant = TenantContext.require();
        long totalLeads = leads.countByTenantId(tenant);
        long wonCount = leads.countByTenantIdAndStage(tenant, LeadStage.WON);
        long activeLeads = leads.countByTenantIdAndStageNotIn(tenant, List.of(LeadStage.WON, LeadStage.LOST));
        BigDecimal revenue = value(leads.sumValueByTenantIdAndStage(tenant, LeadStage.WON));
        long openTasks = tasks.countByTenantIdAndStatusNotIn(tenant, DONE);
        BigDecimal conversion = totalLeads == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(wonCount * 100.0 / totalLeads)
                .setScale(1, RoundingMode.HALF_UP);

        Map<LeadStage, Object[]> pipelineData = leads.pipeline(tenant).stream()
                .collect(Collectors.toMap(row -> (LeadStage) row[0], Function.identity()));
        List<Pipeline> pipeline = Arrays.stream(LeadStage.values()).map(stage -> {
            Object[] row = pipelineData.get(stage);
            return new Pipeline(stage, row == null ? 0 : ((Number) row[1]).longValue(),
                    row == null ? BigDecimal.ZERO : value((BigDecimal) row[2]));
        }).toList();

        Map<TaskStatus, Long> taskCounts = new EnumMap<>(TaskStatus.class);
        tasks.statusSummary(tenant).forEach(row -> taskCounts.put((TaskStatus) row[0], ((Number) row[1]).longValue()));
        List<TaskSummary> taskSummary = Arrays.stream(TaskStatus.values())
                .map(status -> new TaskSummary(status, taskCounts.getOrDefault(status, 0L))).toList();

        List<UpcomingTask> upcoming = tasks.findTop5ByTenantIdAndDueAtIsNotNullAndStatusNotInOrderByDueAtAsc(tenant, DONE).stream()
                .map(t -> new UpcomingTask(t.getId(), t.getTitle(), t.getStatus(), t.getPriority(), t.getDueAt(), t.getAssigneeId()))
                .toList();
        List<ActivityService.ActivityDto> recent = activities.findTop8ByTenantIdOrderByCreatedAtDesc(tenant).stream()
                .map(a -> new ActivityService.ActivityDto(a.getId(), a.getActorId(), a.getAction(), a.getEntityType(),
                        a.getEntityId(), a.getSummary(), a.getCreatedAt())).toList();

        Metrics metrics = new Metrics(revenue, activeLeads, conversion, openTasks,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        YearMonth firstTrendMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(5);
        Instant trendStart = firstTrendMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new Response(metrics, pipeline, revenueTrend(leads.revenueByMonth(tenant, LeadStage.WON, trendStart)),
                taskSummary, recent, totalLeads, customers.countByTenantId(tenant),
                customers.countByTenantIdAndStatus(tenant, CustomerStatus.ACTIVE),
                openTasks, tasks.countByTenantIdAndDueAtBeforeAndStatusNotIn(tenant, Instant.now(), DONE),
                revenue, conversion, upcoming);
    }

    private static List<RevenuePoint> revenueTrend(List<Object[]> monthlyRows) {
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        Map<YearMonth, BigDecimal> revenue = monthlyRows.stream().collect(Collectors.toMap(
                row -> YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                row -> value((BigDecimal) row[2])));
        List<RevenuePoint> points = new ArrayList<>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth month = current.minusMonths(offset);
            points.add(new RevenuePoint(month.format(label), revenue.getOrDefault(month, BigDecimal.ZERO)));
        }
        return points;
    }

    private static BigDecimal value(BigDecimal number) { return number == null ? BigDecimal.ZERO : number; }

    public record Response(Metrics metrics, List<Pipeline> pipeline, List<RevenuePoint> revenueTrend,
                           List<TaskSummary> taskSummary, List<ActivityService.ActivityDto> recentActivities,
                           long totalLeads, long totalCustomers, long activeCustomers, long openTasks, long overdueTasks,
                           BigDecimal wonValue, BigDecimal conversionRate, List<UpcomingTask> upcomingTasks) {}
    public record Metrics(BigDecimal totalRevenue, long activeLeads, BigDecimal conversionRate, long openTasks,
                          BigDecimal revenueChange, BigDecimal leadChange, BigDecimal conversionChange,
                          BigDecimal taskChange) {}
    public record Pipeline(LeadStage stage, long count, BigDecimal value) {}
    public record RevenuePoint(String label, BigDecimal revenue) {}
    public record TaskSummary(TaskStatus status, long count) {}
    public record UpcomingTask(UUID id, String title, TaskStatus status, TaskPriority priority,
                               Instant dueAt, UUID assigneeId) {}
}
