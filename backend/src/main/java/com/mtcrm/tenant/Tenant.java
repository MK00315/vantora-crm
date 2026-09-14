package com.mtcrm.tenant;

import com.mtcrm.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant extends AuditableEntity {
    @Column(nullable = false, length = 140)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "reminder_last_served_at")
    private Instant reminderLastServedAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getReminderLastServedAt() { return reminderLastServedAt; }
    public void setReminderLastServedAt(Instant reminderLastServedAt) { this.reminderLastServedAt = reminderLastServedAt; }
}
