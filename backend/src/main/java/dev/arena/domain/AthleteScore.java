package dev.arena.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "athlete_score")
public class AthleteScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "athlete_id", nullable = false)
    private Long athleteId;

    @Column(nullable = false)
    private double points;

    private String breakdown;

    protected AthleteScore() {}

    public AthleteScore(Long eventId, Long athleteId, double points, String breakdown) {
        this.eventId = eventId;
        this.athleteId = athleteId;
        this.points = points;
        this.breakdown = breakdown;
    }

    public Long getEventId() { return eventId; }
    public Long getAthleteId() { return athleteId; }
    public double getPoints() { return points; }
    public String getBreakdown() { return breakdown; }
}
