package com.mtcrm.task;

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
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }

    @GetMapping
    PageResponse<TaskDtos.Response> list(@RequestParam(defaultValue = "") String q,
                                         @RequestParam(required = false) TaskStatus status,
                                         @RequestParam(required = false) TaskPriority priority,
                                         @RequestParam(required = false) UUID assigneeId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(defaultValue = "dueAt") String sort,
                                         @RequestParam(defaultValue = "asc") String direction) {
        return service.list(q, status, priority, assigneeId, page, size, sort, direction);
    }
    @GetMapping("/{id}") TaskDtos.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    TaskDtos.Response create(@Valid @RequestBody TaskDtos.Request request) { return service.create(request); }

    @PutMapping("/{id}") TaskDtos.Response update(@PathVariable UUID id, @Valid @RequestBody TaskDtos.Request request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    TaskDtos.Response status(@PathVariable UUID id, @Valid @RequestBody TaskDtos.StatusRequest request) {
        return service.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','HR','RECRUITER')")
    void delete(@PathVariable UUID id) { service.delete(id); }
}
