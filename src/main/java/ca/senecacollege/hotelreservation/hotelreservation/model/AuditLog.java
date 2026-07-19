package ca.senecacollege.hotelreservation.hotelreservation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * An administrative activity record for the audit table / activity-log report:
 * who did what, to which entity, and when. Standalone (no FKs) so a log entry
 * survives even if the referenced entity is later deleted.
 * Maps to the {@code audit_log} table.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Username of the admin who performed the action (or "SYSTEM"). */
    @Column(name = "actor", length = 40)
    private String actor;

    /** e.g. "LOGIN", "CREATE_RESERVATION", "PAYMENT", "REFUND", "DISCOUNT". */
    @Column(name = "action", length = 40)
    private String action;

    /** The kind of entity affected, e.g. "Reservation", "Payment". */
    @Column(name = "entity_type", length = 40)
    private String entityType;

    /** The affected entity's identifier, stored as text so any id type fits. */
    @Column(name = "entity_id", length = 40)
    private String entityId;

    @Column(name = "message", length = 500)
    private String message;

    public AuditLog() {
    }

    public AuditLog(String actor, String action, String entityType, String entityId, String message) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
