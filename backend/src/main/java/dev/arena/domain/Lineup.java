package dev.arena.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lineup")
public class Lineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "total_points", nullable = false)
    private double totalPoints;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lineup_pick", joinColumns = @JoinColumn(name = "lineup_id"))
    @Column(name = "athlete_id")
    private Set<Long> athleteIds = new HashSet<>();

    protected Lineup() {}

    public Lineup(Long userId, Long eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getEventId() { return eventId; }
    public double getTotalPoints() { return totalPoints; }
    public Set<Long> getAthleteIds() { return athleteIds; }
    public void setTotalPoints(double totalPoints) { this.totalPoints = totalPoints; }
    public void setAthleteIds(Set<Long> athleteIds) {
        this.athleteIds.clear();
        this.athleteIds.addAll(athleteIds);
    }
}
