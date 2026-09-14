package com.mtcrm.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class CustomerDtos {
    private CustomerDtos() {}

    public record Request(
            @NotBlank @Size(max = 160) String name,
            @Email @Size(max = 254) String email,
            @Size(max = 40) String phone,
            @Size(max = 160) String company,
            @Size(max = 300)
            @Pattern(regexp = "^\\s*$|(?i)^https?://\\S+$", message = "must use an http or https URL") String website,
            @NotNull CustomerStatus status,
            @Size(max = 5000) String notes,
            UUID ownerId
    ) {}

    public record Response(UUID id, String name, String email, String phone, String company, String website,
                           CustomerStatus status, String notes, UUID ownerId, Instant createdAt, Instant updatedAt) {
        public static Response from(Customer c) {
            return new Response(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getCompany(), c.getWebsite(),
                    c.getStatus(), c.getNotes(), c.getOwnerId(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
