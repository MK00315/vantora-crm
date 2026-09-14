package com.mtcrm.activity;

import com.mtcrm.common.api.PageResponse;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.security.CurrentUser;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantQuotaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivityService {
    private final ActivityRepository activities;
    private final TenantQuotaService quotas;

    public ActivityService(ActivityRepository activities, TenantQuotaService quotas) {
        this.activities = activities;
        this.quotas = quotas;
    }

    @Transactional
    public void record(String action, String entityType, UUID entityId, String summary) {
        quotas.prepareActivitySlot();
        var activity = new Activity();
        activity.setTenantId(TenantContext.require());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser user) activity.setActorId(user.id());
        activity.setAction(action);
        activity.setEntityType(entityType);
        activity.setEntityId(entityId);
        activity.setSummary(summary);
        activities.save(activity);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityDto> list(int page, int size) {
        if (page < 0) throw new BadRequestException("page must be zero or greater");
        if (size < 1 || size > 100) throw new BadRequestException("size must be between 1 and 100");
        var result = activities.findAllByTenantId(TenantContext.require(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, ActivityDto::from);
    }

    public record ActivityDto(UUID id, UUID actorId, String action, String entityType, UUID entityId,
                              String summary, java.time.Instant createdAt) {
        static ActivityDto from(Activity a) {
            return new ActivityDto(a.getId(), a.getActorId(), a.getAction(), a.getEntityType(),
                    a.getEntityId(), a.getSummary(), a.getCreatedAt());
        }
    }
}
