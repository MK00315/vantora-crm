package com.mtcrm.lead;

import com.mtcrm.activity.ActivityService;
import com.mtcrm.common.api.PageResponse;
import com.mtcrm.common.api.PageableFactory;
import com.mtcrm.common.exception.BadRequestException;
import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.tenant.TenantContext;
import com.mtcrm.tenant.TenantQuotaService;
import com.mtcrm.user.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class LeadService {
    private static final Set<String> SORTS = Set.of("firstName", "lastName", "company", "stage", "estimatedValue", "createdAt", "updatedAt");
    private final LeadRepository leads;
    private final UserRepository users;
    private final ActivityService activities;
    private final TenantQuotaService quotas;

    public LeadService(LeadRepository leads, UserRepository users, ActivityService activities, TenantQuotaService quotas) {
        this.leads = leads;
        this.users = users;
        this.activities = activities;
        this.quotas = quotas;
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadDtos.Response> list(String query, LeadStage stage, int page, int size, String sort, String direction) {
        var result = leads.search(TenantContext.require(), query == null ? "" : query.trim(), stage,
                PageableFactory.create(page, size, sort, direction, SORTS));
        return PageResponse.from(result, LeadDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public LeadDtos.Response get(UUID id) { return LeadDtos.Response.from(find(id)); }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public LeadDtos.Response create(LeadDtos.Request request) {
        quotas.assertCanCreate(TenantQuotaService.Resource.LEAD);
        validateOwner(request.ownerId());
        Lead lead = new Lead();
        lead.setTenantId(TenantContext.require());
        apply(lead, request);
        leads.save(lead);
        activities.record("CREATED", "LEAD", lead.getId(), "Created lead " + lead.getFirstName() + " " + lead.getLastName());
        return LeadDtos.Response.from(lead);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public LeadDtos.Response update(UUID id, LeadDtos.Request request) {
        validateOwner(request.ownerId());
        Lead lead = find(id);
        apply(lead, request);
        activities.record("UPDATED", "LEAD", id, "Updated lead " + lead.getFirstName() + " " + lead.getLastName());
        return LeadDtos.Response.from(lead);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public LeadDtos.Response changeStage(UUID id, LeadStage stage) {
        Lead lead = find(id);
        LeadStage old = lead.getStage();
        lead.setStage(stage);
        lead.setStageChangedAt(Instant.now());
        activities.record("STAGE_CHANGED", "LEAD", id, "Moved lead from " + old + " to " + stage);
        return LeadDtos.Response.from(lead);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboard", key = "T(com.mtcrm.tenant.TenantContext).require().toString()")
    public void delete(UUID id) {
        Lead lead = find(id);
        leads.delete(lead);
        activities.record("DELETED", "LEAD", id, "Deleted lead " + lead.getFirstName() + " " + lead.getLastName());
    }

    private Lead find(UUID id) {
        return leads.findByIdAndTenantId(id, TenantContext.require()).orElseThrow(() -> new NotFoundException("Lead not found"));
    }

    private void validateOwner(UUID id) {
        if (id != null && users.findByIdAndTenantId(id, TenantContext.require()).isEmpty())
            throw new BadRequestException("Owner must be a user in your company");
    }

    private static void apply(Lead l, LeadDtos.Request r) {
        l.setFirstName(r.firstName().trim()); l.setLastName(r.lastName().trim());
        l.setEmail(blank(r.email())); l.setPhone(blank(r.phone())); l.setCompany(blank(r.company()));
        l.setTitle(blank(r.title())); l.setSource(blank(r.source()));
        if (l.getStage() != r.stage()) l.setStageChangedAt(Instant.now());
        l.setStage(r.stage());
        l.setEstimatedValue(r.estimatedValue() == null ? BigDecimal.ZERO : r.estimatedValue());
        l.setOwnerId(r.ownerId()); l.setNotes(blank(r.notes()));
    }
    private static String blank(String v) { return v == null || v.isBlank() ? null : v.trim(); }
}
