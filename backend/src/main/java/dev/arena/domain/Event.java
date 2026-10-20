package dev.arena.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sport;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.SCHEDULED;

    protected Event() {}

    public Event(String sport, String externalId, String name, Instant startsAt) {
        this.sport = sport;
        this.externalId = externalId;
        this.name = name;
        this.startsAt = startsAt;
    }

    /** Lineups close when the event starts. */
    public boolean isLocked() {
        return !Instant.now().isBefore(startsAt);
    }

    public Long getId() { return id; }
    public String getSport() { return sport; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public Instant getStartsAt() { return startsAt; }
    public EventStatus getStatus() { return status; }
    public void setName(String name) { this.name = name; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public void setStatus(EventStatus status) { this.status = status; }
}
