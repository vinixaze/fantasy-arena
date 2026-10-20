package dev.arena.sport;

import java.util.List;
import java.util.Map;

/**
 * Everything the platform needs to know about a sport. The core (users, leagues,
 * lineups, ranking, prices) only talks to this interface, so adding a sport means
 * implementing it and registering it as a Spring bean.
 */
public interface SportModule {

    String code();

    String displayName();

    LineupRules lineupRules();

    /** Events (rounds, races, cards) of a season. */
    List<EventData> fetchEvents(int season);

    /** Athletes available as of the given event. */
    List<AthleteData> fetchAthletes(String eventExternalId);

    /** Points per athlete external id. Empty while results are not available yet. */
    Map<String, ScoreLine> fetchScores(String eventExternalId);

    /**
     * Price after an event: moves toward a value proportional to the points scored.
     * Sports can override to tune the market.
     */
    default double nextPrice(String kind, double currentPrice, double points) {
        double target = Math.min(40, Math.max(4, points * 0.6));
        double next = currentPrice * 0.7 + target * 0.3;
        return Math.round(next * 10) / 10.0;
    }
}
