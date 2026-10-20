package dev.arena.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "league_member")
public class LeagueMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected LeagueMember() {}

    public LeagueMember(Long leagueId, Long userId) {
        this.leagueId = leagueId;
        this.userId = userId;
    }

    public Long getLeagueId() { return leagueId; }
    public Long getUserId() { return userId; }
}
