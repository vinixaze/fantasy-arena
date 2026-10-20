package dev.arena.sport.f1;

import dev.arena.sport.ScoreLine;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class F1ScoringTest {

    @Test
    void winnerFromPoleScoresTwenty() {
        assertEquals(20, F1Scoring.driver(1, 1, false).points());
    }

    @Test
    void positionsGainedAreRewarded() {
        // P3 from P10: (21 - 3) + 7
        assertEquals(25, F1Scoring.driver(3, 10, false).points());
    }

    @Test
    void positionsLostArePenalised() {
        // P8 from P2: (21 - 8) - 6
        assertEquals(7, F1Scoring.driver(8, 2, false).points());
    }

    @Test
    void dnfIsFlatPenalty() {
        assertEquals(F1Scoring.DNF_PENALTY, F1Scoring.driver(null, 4, true).points());
        assertEquals(F1Scoring.DNF_PENALTY, F1Scoring.driver(12, 4, true).points());
    }

    @Test
    void missingGridIsNeutral() {
        assertEquals(15, F1Scoring.driver(6, null, false).points());
    }

    @Test
    void constructorIsSumOfItsDrivers() {
        Map<String, ScoreLine> drivers = new LinkedHashMap<>();
        drivers.put("VER", F1Scoring.driver(1, 1, false));
        drivers.put("LAW", F1Scoring.driver(null, 5, true));
        drivers.put("HAM", F1Scoring.driver(2, 3, false));
        Map<String, String> teams = Map.of("VER", "Red Bull", "LAW", "Red Bull", "HAM", "Ferrari");

        Map<String, ScoreLine> all = F1Scoring.withConstructors(drivers, teams);

        assertEquals(10, all.get(F1Scoring.teamKey("Red Bull")).points());
        assertEquals(20, all.get(F1Scoring.teamKey("Ferrari")).points());
        assertFalse(all.isEmpty());
    }
}
