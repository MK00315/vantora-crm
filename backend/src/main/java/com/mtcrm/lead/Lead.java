package com.mtcrm.lead;

import com.mtcrm.common.domain.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leads")
public class Lead extends TenantOwnedEntity {
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;
    @Column(length = 254)
    private String email;
    @Column(length = 40)
    private String phone;
    @Column(length = 160)
    private String company;
    @Column(length = 120)
    private String title;
    @Column(length = 80)
    private String source;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStage stage = LeadStage.NEW;
    @Column(name = "estimated_value", precision = 14, scale = 2)
    private BigDecimal estimatedValue = BigDecimal.ZERO;
    @Column(name = "owner_id")
    private UUID ownerId;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(name = "stage_changed_at", nullable = false)
    private Instant stageChangedAt = Instant.now();

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LeadStage getStage() { return stage; }
    public void setStage(LeadStage stage) { this.stage = stage; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getStageChangedAt() { return stageChangedAt; }
    public void setStageChangedAt(Instant stageChangedAt) { this.stageChangedAt = stageChangedAt; }
}
