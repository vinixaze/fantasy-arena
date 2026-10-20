package dev.arena.sport.football;

import com.fasterxml.jackson.databind.JsonNode;
import dev.arena.sport.*;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class FootballModule implements SportModule {
    public static final String CODE = "FOOTBALL";
    private final SportmonksClient client;
    public FootballModule(SportmonksClient client) { this.client = client; }
    public String code() { return CODE; }
    public String displayName() { return client.configured() ? "Futebol" : "Futebol (configure a SportMonks)"; }
    public LineupRules lineupRules() {
        Map<String,Integer> slots = new LinkedHashMap<>();
        slots.put("GOALKEEPER", 1); slots.put("DEFENDER", 4); slots.put("MIDFIELDER", 4); slots.put("ATTACKER", 2);
        return new LineupRules(100, slots);
    }

    public List<EventData> fetchEvents(int season) {
        LocalDate cursor = LocalDate.of(season, 1, 1), yearEnd = LocalDate.of(season, 12, 31);
        List<JsonNode> fixtures = new ArrayList<>();
        while (!cursor.isAfter(yearEnd)) {
            LocalDate end = cursor.plusDays(89).isAfter(yearEnd) ? yearEnd : cursor.plusDays(89);
            fixtures.addAll(client.fixtures(cursor.toString(), end.toString()));
            cursor = end.plusDays(1);
        }
        return fixtures.stream().map(f ->
                new EventData(f.path("id").asText(), f.path("name").asText("Partida"), parseInstant(f.path("starting_at").asText())))
                .collect(java.util.stream.Collectors.toMap(EventData::externalId, e -> e, (a,b) -> a,
                        LinkedHashMap::new)).values().stream().toList();
    }

    public List<AthleteData> fetchAthletes(String eventExternalId) {
        JsonNode fixture = client.fixture(Long.parseLong(eventExternalId));
        if (fixture == null) return List.of();
        List<AthleteData> out = new ArrayList<>();
        for (JsonNode lineup : fixture.path("lineups")) {
            JsonNode player = lineup.path("player");
            if (player.isMissingNode()) continue;
            String position = position(player.path("position").path("name").asText());
            String team = participantName(fixture.path("participants"), lineup.path("participant_id").asLong());
            out.add(new AthleteData(player.path("id").asText(), player.path("display_name").asText(player.path("name").asText()),
                    team, position, player.path("image_path").asText(null), 8));
        }
        return out.stream().collect(java.util.stream.Collectors.toMap(AthleteData::externalId, a -> a, (a,b) -> a,
                LinkedHashMap::new)).values().stream().toList();
    }

    public Map<String, ScoreLine> fetchScores(String eventExternalId) {
        JsonNode fixture = client.fixture(Long.parseLong(eventExternalId));
        if (fixture == null || !finished(fixture)) return Map.of();
        Map<String, ScoreLine> result = new LinkedHashMap<>();
        for (JsonNode lineup : fixture.path("lineups")) {
            JsonNode player = lineup.path("player");
            if (player.isMissingNode()) continue;
            Map<String, Double> breakdown = new LinkedHashMap<>();
            double points = 0;
            for (JsonNode detail : lineup.path("details")) {
                String type = detail.path("type").path("developer_name").asText("").toUpperCase();
                double value = detail.path("data").path("value").asDouble(detail.path("value").asDouble());
                double scored = switch (type) {
                    case "GOALS" -> value * 5; case "ASSISTS" -> value * 3;
                    case "YELLOWCARDS", "YELLOW_CARDS" -> value * -1;
                    case "REDCARDS", "RED_CARDS" -> value * -3;
                    case "PENALTIES_MISSED" -> value * -2; case "PENALTIES_SAVED" -> value * 5;
                    default -> 0;
                };
                if (scored != 0) { breakdown.put(type, scored); points += scored; }
            }
            result.put(player.path("id").asText(), new ScoreLine(points, breakdown));
        }
        return result;
    }

    private static boolean finished(JsonNode f) {
        int state = f.path("state_id").asInt();
        return state == 5 || state == 7 || f.path("result_info").isTextual();
    }
    private static String participantName(JsonNode participants, long id) {
        for (JsonNode p : participants) if (p.path("id").asLong() == id) return p.path("name").asText();
        return null;
    }
    private static String position(String value) {
        String p = value.toLowerCase();
        if (p.contains("goal")) return "GOALKEEPER";
        if (p.contains("def")) return "DEFENDER";
        if (p.contains("mid")) return "MIDFIELDER";
        return "ATTACKER";
    }
    private static Instant parseInstant(String value) {
        try { return OffsetDateTime.parse(value).toInstant(); }
        catch (Exception ignored) { return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC); }
    }
}
