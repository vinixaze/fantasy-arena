package dev.arena.sport.f1;

import dev.arena.sport.AthleteData;
import dev.arena.sport.EventData;
import dev.arena.sport.LineupRules;
import dev.arena.sport.ScoreLine;
import dev.arena.sport.SportModule;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class F1Module implements SportModule {

    public static final String CODE = "F1";
    static final double DRIVER_BASE_PRICE = 8;
    static final double TEAM_BASE_PRICE = 16;

    private final OpenF1Client client;

    public F1Module(OpenF1Client client) {
        this.client = client;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String displayName() {
        return "Fórmula 1";
    }

    @Override
    public LineupRules lineupRules() {
        Map<String, Integer> slots = new LinkedHashMap<>();
        slots.put("DRIVER", 2);
        slots.put("TEAM", 1);
        return new LineupRules(45, slots);
    }

    @Override
    public List<EventData> fetchEvents(int season) {
        return client.races(season).stream()
                .map(s -> new EventData(
                        String.valueOf(s.sessionKey()),
                        "GP " + s.location(),
                        OffsetDateTime.parse(s.dateStart()).toInstant()))
                .toList();
    }

    @Override
    public List<AthleteData> fetchAthletes(String eventExternalId) {
        List<OpenF1Client.Driver> drivers = client.drivers(Long.parseLong(eventExternalId));
        List<AthleteData> athletes = new ArrayList<>();
        Set<String> teams = new LinkedHashSet<>();
        for (OpenF1Client.Driver d : drivers) {
            if (d.acronym() == null) continue;
            athletes.add(new AthleteData(d.acronym(), d.fullName(), d.teamName(), "DRIVER",
                    d.headshotUrl(), DRIVER_BASE_PRICE));
            if (d.teamName() != null) teams.add(d.teamName());
        }
        for (String team : teams) {
            athletes.add(new AthleteData(F1Scoring.teamKey(team), team, team, "TEAM", null, TEAM_BASE_PRICE));
        }
        return athletes;
    }

    @Override
    public Map<String, ScoreLine> fetchScores(String eventExternalId) {
        long sessionKey = Long.parseLong(eventExternalId);
        List<OpenF1Client.Result> results = client.results(sessionKey);
        if (results.isEmpty()) {
            return Map.of();
        }
        Map<Integer, OpenF1Client.Driver> byNumber = client.drivers(sessionKey).stream()
                .collect(Collectors.toMap(OpenF1Client.Driver::driverNumber, Function.identity(), (a, b) -> a));
        Map<Integer, Integer> gridByNumber = new LinkedHashMap<>();
        for (OpenF1Client.GridSlot g : client.startingGrid(sessionKey)) {
            if (g.position() != null) gridByNumber.put(g.driverNumber(), g.position());
        }

        Map<String, ScoreLine> drivers = new LinkedHashMap<>();
        Map<String, String> teamByDriver = new LinkedHashMap<>();
        for (OpenF1Client.Result r : results) {
            OpenF1Client.Driver d = byNumber.get(r.driverNumber());
            if (d == null || d.acronym() == null) continue;
            boolean out = Boolean.TRUE.equals(r.dnf()) || Boolean.TRUE.equals(r.dns()) || Boolean.TRUE.equals(r.dsq());
            drivers.put(d.acronym(), F1Scoring.driver(r.position(), gridByNumber.get(r.driverNumber()), out));
            teamByDriver.put(d.acronym(), d.teamName());
        }
        return F1Scoring.withConstructors(drivers, teamByDriver);
    }
}
