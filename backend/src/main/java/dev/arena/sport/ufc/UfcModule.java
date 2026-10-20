package dev.arena.sport.ufc;

import dev.arena.sport.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** Manual sport: data is maintained by administrators instead of an external API. */
@Component
public class UfcModule implements SportModule {
    public static final String CODE = "UFC";
    public String code() { return CODE; }
    public String displayName() { return "UFC"; }
    public LineupRules lineupRules() { return new LineupRules(100, Map.of("FIGHTER", 5)); }
    public List<EventData> fetchEvents(int season) { return List.of(); }
    public List<AthleteData> fetchAthletes(String eventExternalId) { return List.of(); }
    public Map<String, ScoreLine> fetchScores(String eventExternalId) { return Map.of(); }
}
