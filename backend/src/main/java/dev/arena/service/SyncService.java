package dev.arena.service;

import dev.arena.domain.Athlete;
import dev.arena.domain.Event;
import dev.arena.domain.EventStatus;
import dev.arena.repo.AthleteRepository;
import dev.arena.repo.EventRepository;
import dev.arena.sport.AthleteData;
import dev.arena.sport.EventData;
import dev.arena.sport.ScoreLine;
import dev.arena.sport.SportModule;
import dev.arena.sport.SportRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps events, athletes and results in sync with each sport's data source.
 * Past events that were never scored are processed oldest first, which also seeds athlete prices.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final Duration RESULT_DELAY = Duration.ofHours(3);

    private final SportRegistry registry;
    private final EventRepository events;
    private final AthleteRepository athletes;
    private final ScoringService scoring;

    public SyncService(SportRegistry registry, EventRepository events, AthleteRepository athletes, ScoringService scoring) {
        this.registry = registry;
        this.events = events;
        this.athletes = athletes;
        this.scoring = scoring;
    }

    @Scheduled(initialDelay = 5_000, fixedDelayString = "${arena.sync.interval-ms:3600000}")
    public synchronized void syncAll() {
        for (SportModule module : registry.all()) {
            try {
                sync(module);
            } catch (Exception e) {
                log.warn("Sync failed for {}: {}", module.code(), e.getMessage());
            }
        }
    }

    void sync(SportModule module) {
        int year = Year.now(ZoneOffset.UTC).getValue();
        List<EventData> season = module.fetchEvents(year);
        season.forEach(data -> upsertEvent(module.code(), data));
        refreshRoster(module, season, year);
        scorePending(module);
    }

    private void upsertEvent(String sport, EventData data) {
        Event event = events.findBySportAndExternalId(sport, data.externalId())
                .orElseGet(() -> new Event(sport, data.externalId(), data.name(), data.startsAt()));
        event.setName(data.name());
        event.setStartsAt(data.startsAt());
        events.save(event);
    }

    private void refreshRoster(SportModule module, List<EventData> season, int year) {
        Instant now = Instant.now();
        Optional<EventData> source = latestPast(season, now);
        if (source.isEmpty()) {
            // season has not started: use the last event of the previous year
            source = latestPast(module.fetchEvents(year - 1), now);
        }
        if (source.isEmpty()) {
            return;
        }
        for (AthleteData data : module.fetchAthletes(source.get().externalId())) {
            Athlete athlete = athletes.findBySportAndExternalId(module.code(), data.externalId())
                    .orElseGet(() -> new Athlete(module.code(), data.externalId(), data.name(), data.team(),
                            data.kind(), data.imageUrl(), data.basePrice()));
            athlete.setName(data.name());
            athlete.setTeam(data.team());
            athlete.setImageUrl(data.imageUrl());
            athletes.save(athlete);
        }
    }

    private Optional<EventData> latestPast(List<EventData> list, Instant now) {
        return list.stream()
                .filter(e -> !e.startsAt().isAfter(now))
                .max(Comparator.comparing(EventData::startsAt));
    }

    private void scorePending(SportModule module) {
        Instant cutoff = Instant.now().minus(RESULT_DELAY);
        List<Event> pending = events.findBySportAndStatusAndStartsAtBeforeOrderByStartsAtAsc(
                module.code(), EventStatus.SCHEDULED, cutoff);
        for (Event event : pending) {
            Map<String, ScoreLine> result = module.fetchScores(event.getExternalId());
            if (!result.isEmpty()) {
                scoring.apply(event.getId(), result);
            }
        }
    }
}
