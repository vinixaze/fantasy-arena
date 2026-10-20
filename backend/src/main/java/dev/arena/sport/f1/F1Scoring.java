package dev.arena.sport.f1;

import dev.arena.sport.ScoreLine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scoring rules for a race:
 * - finishing position: 21 - position (P1 = 20 ... P20 = 1)
 * - positions gained versus the starting grid: +1 per place (negative if lost)
 * - did not finish: flat -10
 * A constructor scores the sum of its drivers.
 */
public final class F1Scoring {

    public static final double DNF_PENALTY = -10;

    private F1Scoring() {}

    public static ScoreLine driver(Integer finish, Integer grid, boolean dnf) {
        Map<String, Double> breakdown = new LinkedHashMap<>();
        if (dnf || finish == null) {
            breakdown.put("dnf", DNF_PENALTY);
            return new ScoreLine(DNF_PENALTY, breakdown);
        }
        double finishPoints = Math.max(0, 21 - finish);
        double gained = (grid == null || grid <= 0) ? 0 : grid - finish;
        breakdown.put("finish", finishPoints);
        breakdown.put("positionsGained", gained);
        return new ScoreLine(finishPoints + gained, breakdown);
    }

    /** Adds one entry per constructor (key "team:NAME") to the driver scores. */
    public static Map<String, ScoreLine> withConstructors(Map<String, ScoreLine> drivers,
                                                          Map<String, String> teamByDriver) {
        Map<String, ScoreLine> all = new LinkedHashMap<>(drivers);
        Map<String, Double> teamTotals = new HashMap<>();
        drivers.forEach((driverId, score) -> {
            String team = teamByDriver.get(driverId);
            if (team != null) {
                teamTotals.merge(team, score.points(), Double::sum);
            }
        });
        teamTotals.forEach((team, total) ->
                all.put(teamKey(team), new ScoreLine(total, Map.of("drivers", total))));
        return all;
    }

    public static String teamKey(String teamName) {
        return "team:" + teamName;
    }
}
