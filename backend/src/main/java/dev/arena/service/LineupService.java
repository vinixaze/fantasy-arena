package dev.arena.service;

import dev.arena.api.Dtos.LineupDto;
import dev.arena.api.Dtos.LineupSummary;
import dev.arena.api.Dtos.PickDto;
import dev.arena.domain.Athlete;
import dev.arena.domain.AthleteScore;
import dev.arena.domain.Event;
import dev.arena.domain.EventStatus;
import dev.arena.domain.Lineup;
import dev.arena.repo.AthleteRepository;
import dev.arena.repo.AthleteScoreRepository;
import dev.arena.repo.EventRepository;
import dev.arena.repo.LineupRepository;
import dev.arena.sport.LineupRules;
import dev.arena.sport.SportRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LineupService {

    private final EventRepository events;
    private final AthleteRepository athletes;
    private final AthleteScoreRepository scores;
    private final LineupRepository lineups;
    private final SportRegistry registry;

    public LineupService(EventRepository events, AthleteRepository athletes, AthleteScoreRepository scores,
                         LineupRepository lineups, SportRegistry registry) {
        this.events = events;
        this.athletes = athletes;
        this.scores = scores;
        this.lineups = lineups;
        this.registry = registry;
    }

    @Transactional
    public LineupDto submit(long userId, long eventId, List<Long> athleteIds) {
        Event event = events.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));
        if (event.isLocked() || event.getStatus() != EventStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O mercado deste evento já fechou");
        }
        LineupRules rules = registry.find(event.getSport()).orElseThrow().lineupRules();

        if (new HashSet<>(athleteIds).size() != athleteIds.size()) {
            throw bad("Atletas repetidos na escalação");
        }
        List<Athlete> picked = athletes.findAllById(athleteIds);
        if (picked.size() != athleteIds.size()) {
            throw bad("Atleta inexistente na escalação");
        }
        if (picked.stream().anyMatch(a -> !a.getSport().equals(event.getSport()))) {
            throw bad("Atleta de outro esporte");
        }

        Map<String, Long> countByKind = picked.stream()
                .collect(Collectors.groupingBy(Athlete::getKind, Collectors.counting()));
        for (Map.Entry<String, Integer> slot : rules.slots().entrySet()) {
            long have = countByKind.getOrDefault(slot.getKey(), 0L);
            if (have != slot.getValue()) {
                throw bad("Escolha exatamente " + slot.getValue() + " (" + slot.getKey() + ")");
            }
        }
        if (countByKind.size() != rules.slots().size()) {
            throw bad("Escalação com atletas de tipo inválido");
        }
        double spent = picked.stream().mapToDouble(Athlete::getPrice).sum();
        if (spent > rules.budget() + 1e-9) {
            throw bad("Orçamento excedido");
        }

        Lineup lineup = lineups.findByUserIdAndEventId(userId, eventId).orElseGet(() -> new Lineup(userId, eventId));
        lineup.setAthleteIds(new HashSet<>(athleteIds));
        lineups.save(lineup);
        return toDto(lineup, event);
    }

    @Transactional(readOnly = true)
    public Optional<LineupDto> find(long userId, long eventId) {
        Optional<Event> event = events.findById(eventId);
        if (event.isEmpty()) {
            return Optional.empty();
        }
        return lineups.findByUserIdAndEventId(userId, eventId).map(l -> toDto(l, event.get()));
    }

    @Transactional(readOnly = true)
    public List<LineupSummary> history(long userId) {
        List<Lineup> mine = lineups.findByUserId(userId);
        Map<Long, Event> byId = events.findAllById(mine.stream().map(Lineup::getEventId).toList()).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        return mine.stream()
                .filter(l -> byId.containsKey(l.getEventId()))
                .map(l -> {
                    Event e = byId.get(l.getEventId());
                    return new LineupSummary(e.getId(), e.getName(), e.getStartsAt(),
                            e.getStatus() == EventStatus.SCORED, l.getTotalPoints());
                })
                .sorted(Comparator.comparing(LineupSummary::startsAt).reversed())
                .toList();
    }

    private LineupDto toDto(Lineup lineup, Event event) {
        boolean scored = event.getStatus() == EventStatus.SCORED;
        Map<Long, Double> points = new HashMap<>();
        if (scored) {
            for (AthleteScore s : scores.findByEventId(event.getId())) {
                points.put(s.getAthleteId(), s.getPoints());
            }
        }
        List<PickDto> picks = athletes.findAllById(lineup.getAthleteIds()).stream()
                .sorted(Comparator.comparing(Athlete::getKind).thenComparing(Athlete::getName))
                .map(a -> new PickDto(a.getId(), a.getName(), a.getTeam(), a.getKind(),
                        scored ? points.getOrDefault(a.getId(), 0.0) : null))
                .toList();
        return new LineupDto(event.getId(), event.getName(), scored, lineup.getTotalPoints(), picks);
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
