package com.mtcrm.activity;

import com.mtcrm.common.domain.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "activities")
public class Activity extends TenantOwnedEntity {
    @Column(name = "actor_id")
    private UUID actorId;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;
    @Column(name = "entity_id")
    private UUID entityId;
    @Column(nullable = false, length = 300)
    private String summary;

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
