package com.mtcrm.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}
    public record InviteRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotNull Role role
    ) {}
    public record UpdateRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotNull Role role,
            @NotNull UserStatus status
    ) {}
    public record ProfileRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName
    ) {}
    public record PasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 10, max = 72)
            @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "must contain upper-case, lower-case and number")
            String newPassword
    ) {}
    public record Response(UUID id, String firstName, String lastName, String email, Role role, UserStatus status,
                           Instant emailVerifiedAt, Instant lastLoginAt, Instant createdAt, Instant updatedAt) {
        public static Response from(AppUser u) {
            return new Response(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getRole(), u.getStatus(),
                    u.getEmailVerifiedAt(), u.getLastLoginAt(), u.getCreatedAt(), u.getUpdatedAt());
        }
    }
}
