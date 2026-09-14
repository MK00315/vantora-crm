package com.mtcrm.lead;

import com.mtcrm.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {
    private final LeadService service;
    public LeadController(LeadService service) { this.service = service; }

    @GetMapping
    PageResponse<LeadDtos.Response> list(@RequestParam(defaultValue = "") String q,
                                         @RequestParam(required = false) LeadStage stage,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size,
                                         @RequestParam(defaultValue = "createdAt") String sort,
                                         @RequestParam(defaultValue = "desc") String direction) {
        return service.list(q, stage, page, size, sort, direction);
    }
    @GetMapping("/{id}") LeadDtos.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','HR','RECRUITER')")
    LeadDtos.Response create(@Valid @RequestBody LeadDtos.Request request) { return service.create(request); }

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','HR','RECRUITER')")
    LeadDtos.Response update(@PathVariable UUID id, @Valid @RequestBody LeadDtos.Request request) { return service.update(id, request); }

    @PatchMapping("/{id}/stage") @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','HR','RECRUITER')")
    LeadDtos.Response stage(@PathVariable UUID id, @Valid @RequestBody LeadDtos.StageRequest request) {
        return service.changeStage(id, request.stage());
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','HR')")
    void delete(@PathVariable UUID id) { service.delete(id); }
}
