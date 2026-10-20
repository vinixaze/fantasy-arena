package dev.arena.service;

import dev.arena.api.Dtos.LeagueDto;
import dev.arena.api.Dtos.RankingRow;
import dev.arena.domain.Event;
import dev.arena.domain.League;
import dev.arena.domain.LeagueMember;
import dev.arena.domain.Lineup;
import dev.arena.domain.User;
import dev.arena.repo.EventRepository;
import dev.arena.repo.LeagueMemberRepository;
import dev.arena.repo.LeagueRepository;
import dev.arena.repo.LineupRepository;
import dev.arena.repo.UserRepository;
import dev.arena.sport.SportRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LeagueService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LeagueRepository leagues;
    private final LeagueMemberRepository members;
    private final UserRepository users;
    private final EventRepository events;
    private final LineupRepository lineups;
    private final SportRegistry registry;

    public LeagueService(LeagueRepository leagues, LeagueMemberRepository members, UserRepository users,
                         EventRepository events, LineupRepository lineups, SportRegistry registry) {
        this.leagues = leagues;
        this.members = members;
        this.users = users;
        this.events = events;
        this.lineups = lineups;
        this.registry = registry;
    }

    @Transactional
    public LeagueDto create(long userId, String name, String sport) {
        if (registry.find(sport).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esporte desconhecido");
        }
        League league = leagues.save(new League(name.trim(), sport, newInviteCode(), userId));
        members.save(new LeagueMember(league.getId(), userId));
        return toDto(league);
    }

    @Transactional
    public LeagueDto join(long userId, String inviteCode) {
        League league = leagues.findByInviteCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de convite inválido"));
        if (!members.existsByLeagueIdAndUserId(league.getId(), userId)) {
            members.save(new LeagueMember(league.getId(), userId));
        }
        return toDto(league);
    }

    @Transactional(readOnly = true)
    public List<LeagueDto> mine(long userId) {
        List<Long> ids = members.findByUserId(userId).stream().map(LeagueMember::getLeagueId).toList();
        return leagues.findAllById(ids).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RankingRow> ranking(long userId, long leagueId) {
        League league = leagues.findById(leagueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Liga não encontrada"));
        if (!members.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta liga");
        }
        List<Long> userIds = members.findByLeagueId(leagueId).stream().map(LeagueMember::getUserId).toList();
        Set<Long> eventIds = events.findBySportOrderByStartsAtDesc(league.getSport()).stream()
                .map(Event::getId).collect(Collectors.toSet());
        Map<Long, List<Lineup>> byUser = eventIds.isEmpty() ? Map.of()
                : lineups.findByUserIdInAndEventIdIn(userIds, eventIds).stream()
                        .collect(Collectors.groupingBy(Lineup::getUserId));

        return users.findAllById(userIds).stream()
                .map(u -> row(u, byUser.getOrDefault(u.getId(), List.of())))
                .sorted(Comparator.comparingDouble(RankingRow::points).reversed()
                        .thenComparing(RankingRow::username))
                .toList();
    }

    private RankingRow row(User user, List<Lineup> played) {
        double total = played.stream().mapToDouble(Lineup::getTotalPoints).sum();
        return new RankingRow(user.getUsername(), total, played.size());
    }

    private LeagueDto toDto(League league) {
        int size = members.findByLeagueId(league.getId()).size();
        return new LeagueDto(league.getId(), league.getName(), league.getSport(), league.getInviteCode(), size);
    }

    private String newInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (leagues.existsByInviteCode(code));
        return code;
    }
}
