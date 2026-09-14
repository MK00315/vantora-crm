package com.mtcrm.user;

import com.mtcrm.common.api.PageResponse;
import com.mtcrm.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @GetMapping
    PageResponse<UserDtos.Response> list(@RequestParam(defaultValue = "") String q,
                                         @RequestParam(required = false) Role role,
                                         @RequestParam(required = false) UserStatus status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(defaultValue = "createdAt") String sort,
                                         @RequestParam(defaultValue = "desc") String direction) {
        return service.list(q, role, status, page, size, sort, direction);
    }
    @GetMapping("/{id}") UserDtos.Response get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN')")
    UserDtos.Response invite(@AuthenticationPrincipal CurrentUser actor,
                             @Valid @RequestBody UserDtos.InviteRequest request) {
        return service.invite(actor, request);
    }

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN')")
    UserDtos.Response update(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor,
                             @Valid @RequestBody UserDtos.UpdateRequest request) {
        return service.update(id, actor, request);
    }

    @PutMapping("/me")
    UserDtos.Response updateProfile(@AuthenticationPrincipal CurrentUser actor,
                                    @Valid @RequestBody UserDtos.ProfileRequest request) {
        return service.updateProfile(actor.id(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal CurrentUser actor,
                        @Valid @RequestBody UserDtos.PasswordRequest request) {
        service.changePassword(actor.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN')")
    void deactivate(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor) {
        service.deactivate(id, actor);
    }
}
