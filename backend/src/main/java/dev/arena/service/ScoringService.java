package dev.arena.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.arena.domain.AthleteScore;
import dev.arena.domain.Event;
import dev.arena.domain.EventStatus;
import dev.arena.repo.AthleteRepository;
import dev.arena.repo.AthleteScoreRepository;
import dev.arena.repo.EventRepository;
import dev.arena.repo.LineupRepository;
import dev.arena.sport.ScoreLine;
import dev.arena.sport.SportModule;
import dev.arena.sport.SportRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final EventRepository events;
    private final AthleteRepository athletes;
    private final AthleteScoreRepository scores;
    private final LineupRepository lineups;
    private final SportRegistry registry;
    private final ObjectMapper mapper;

    public ScoringService(EventRepository events, AthleteRepository athletes, AthleteScoreRepository scores,
                          LineupRepository lineups, SportRegistry registry, ObjectMapper mapper) {
        this.events = events;
        this.athletes = athletes;
        this.scores = scores;
        this.lineups = lineups;
        this.registry = registry;
        this.mapper = mapper;
    }

    /** Stores athlete points, totals every lineup of the event, moves prices and closes the event. */
    @Transactional
    public void apply(long eventId, Map<String, ScoreLine> result) {
        Event event = events.findById(eventId).orElseThrow();
        if (event.getStatus() == EventStatus.SCORED) {
            return;
        }
        SportModule module = registry.find(event.getSport()).orElseThrow();
        Map<Long, Double> pointsByAthlete = new HashMap<>();

        result.forEach((externalId, line) ->
                athletes.findBySportAndExternalId(event.getSport(), externalId).ifPresentOrElse(athlete -> {
                    scores.save(new AthleteScore(event.getId(), athlete.getId(), line.points(), toJson(line.breakdown())));
                    pointsByAthlete.put(athlete.getId(), line.points());
                    athlete.setPrice(module.nextPrice(athlete.getKind(), athlete.getPrice(), line.points()));
                    athletes.save(athlete);
                }, () -> log.debug("Skipping unknown athlete {} for event {}", externalId, event.getName())));

        lineups.findByEventId(eventId).forEach(lineup -> {
            double total = lineup.getAthleteIds().stream()
                    .mapToDouble(id -> pointsByAthlete.getOrDefault(id, 0.0)).sum();
            lineup.setTotalPoints(total);
            lineups.save(lineup);
        });

        event.setStatus(EventStatus.SCORED);
        events.save(event);
        log.info("Scored {} ({} athletes)", event.getName(), pointsByAthlete.size());
    }

    private String toJson(Map<String, Double> breakdown) {
        try {
            return mapper.writeValueAsString(breakdown);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
