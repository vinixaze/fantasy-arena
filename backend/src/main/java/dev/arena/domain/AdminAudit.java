package dev.arena.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "admin_audit")
public class AdminAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "admin_id", nullable = false) private Long adminId;
    @Column(nullable = false) private String action;
    @Column(name = "entity_type", nullable = false) private String entityType;
    @Column(name = "entity_id") private String entityId;
    private String details;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected AdminAudit() {}
    public AdminAudit(Long adminId, String action, String entityType, String entityId, String details) {
        this.adminId = adminId; this.action = action; this.entityType = entityType;
        this.entityId = entityId; this.details = details;
    }
    public Long getId() { return id; }
    public Long getAdminId() { return adminId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
