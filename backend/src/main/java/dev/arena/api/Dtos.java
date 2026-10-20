package dev.arena.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Dtos {

    private Dtos() {}

    public record AuthRequest(@NotBlank @Size(min = 3, max = 30) String username,
                              @NotBlank @Size(min = 6, max = 72) String password) {}

    public record AuthResponse(String token, String username, String role) {}

    public record MeDto(Long id, String username, String role) {}

    public record SportDto(String code, String name, double budget, Map<String, Integer> slots) {}

    public record EventDto(Long id, String name, Instant startsAt, String status, boolean locked) {}

    public record AthleteDto(Long id, String name, String team, String kind, double price, String imageUrl) {}

    public record LineupRequest(@NotNull @Size(min = 1, max = 20) List<Long> athleteIds) {}

    public record PickDto(Long id, String name, String team, String kind, Double points) {}

    public record LineupDto(Long eventId, String eventName, boolean scored, double totalPoints, List<PickDto> picks) {}

    public record LineupSummary(Long eventId, String eventName, Instant startsAt, boolean scored, double totalPoints) {}

    public record LeagueCreateRequest(@NotBlank @Size(max = 60) String name, @NotBlank String sport) {}

    public record JoinRequest(@NotBlank String inviteCode) {}

    public record LeagueDto(Long id, String name, String sport, String inviteCode, int members) {}

    public record RankingRow(String username, double points, int rounds) {}

    public record AdminUserDto(Long id, String username, String role) {}
    public record RoleRequest(@NotBlank String role) {}
    public record AdminDashboardDto(long users, long events, long athletes, long leagues, boolean sportmonksConfigured) {}
    public record AdminAthleteRequest(@NotBlank @Size(max = 120) String name, String team,
                                      @NotBlank String kind, String imageUrl, double price) {}
    public record AdminEventRequest(@NotBlank @Size(max = 120) String name, @NotNull Instant startsAt) {}
    public record AdminScoreRequest(@NotNull Map<Long, Double> points) {}
    public record AuditDto(String action, String entityType, String entityId, String details, Instant createdAt) {}
}
