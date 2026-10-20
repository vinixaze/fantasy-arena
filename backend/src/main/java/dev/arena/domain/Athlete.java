package dev.arena.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "athlete")
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sport;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    private String team;

    @Column(nullable = false)
    private String kind;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private double price;

    protected Athlete() {}

    public Athlete(String sport, String externalId, String name, String team, String kind, String imageUrl, double price) {
        this.sport = sport;
        this.externalId = externalId;
        this.name = name;
        this.team = team;
        this.kind = kind;
        this.imageUrl = imageUrl;
        this.price = price;
    }

    public Long getId() { return id; }
    public String getSport() { return sport; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getTeam() { return team; }
    public String getKind() { return kind; }
    public String getImageUrl() { return imageUrl; }
    public double getPrice() { return price; }
    public void setName(String name) { this.name = name; }
    public void setTeam(String team) { this.team = team; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setPrice(double price) { this.price = price; }
    public void setKind(String kind) { this.kind = kind; }
}
