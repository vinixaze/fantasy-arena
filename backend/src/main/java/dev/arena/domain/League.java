package dev.arena.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "league")
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sport;

    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    protected League() {}

    public League(String name, String sport, String inviteCode, Long ownerId) {
        this.name = name;
        this.sport = sport;
        this.inviteCode = inviteCode;
        this.ownerId = ownerId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSport() { return sport; }
    public String getInviteCode() { return inviteCode; }
    public Long getOwnerId() { return ownerId; }
}
