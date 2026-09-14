package com.mtcrm.lead;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class LeadDtos {
    private LeadDtos() {}
    public record Request(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @Email @Size(max = 254) String email,
            @Size(max = 40) String phone,
            @Size(max = 160) String company,
            @Size(max = 120) String title,
            @Size(max = 80) String source,
            @NotNull LeadStage stage,
            @DecimalMin("0.0") BigDecimal estimatedValue,
            UUID ownerId,
            @Size(max = 5000) String notes
    ) {}
    public record StageRequest(@NotNull LeadStage stage) {}
    public record Response(UUID id, String firstName, String lastName, String email, String phone, String company,
                           String title, String source, LeadStage stage, BigDecimal estimatedValue, UUID ownerId,
                           String notes, Instant createdAt, Instant updatedAt) {
        public static Response from(Lead l) {
            return new Response(l.getId(), l.getFirstName(), l.getLastName(), l.getEmail(), l.getPhone(), l.getCompany(),
                    l.getTitle(), l.getSource(), l.getStage(), l.getEstimatedValue(), l.getOwnerId(), l.getNotes(),
                    l.getCreatedAt(), l.getUpdatedAt());
        }
    }
}
