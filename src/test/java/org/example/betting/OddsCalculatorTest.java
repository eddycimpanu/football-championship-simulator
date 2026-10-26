package org.example.betting;

import org.example.TestData;
import org.example.engine.TeamCopier;
import org.example.model.Player;
import org.example.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsCalculatorTest {

    private static final int SIMULATIONS = 300;
    private final OddsCalculator calculator = new OddsCalculator(SIMULATIONS, 0.05);

    private static double sum(MatchOdds odds, Selection... selections) {
        double total = 0;
        for (Selection selection : selections) {
            total += odds.probabilityOf(selection);
        }
        return total;
    }

    @Test
    void theChancesOfEachMarketAddUpToOneHundredPercent() {
        MatchOdds odds = calculator.calculate(1, TestData.team("Home", 80), TestData.team("Away", 80), null, 5);

        assertEquals(1.0, sum(odds, Selection.HOME, Selection.DRAW, Selection.AWAY), 1e-9);
        assertEquals(1.0, sum(odds, Selection.OVER_2_5, Selection.UNDER_2_5), 1e-9);
        assertEquals(1.0, sum(odds, Selection.BOTH_SCORE, Selection.NOT_BOTH_SCORE), 1e-9);
    }

    @Test
    void theStrongerTeamIsTheFavourite() {
        MatchOdds odds = calculator.calculate(1, TestData.team("Strong", 88), TestData.team("Weak", 72), null, 5);

        assertTrue(odds.probabilityOf(Selection.HOME) > odds.probabilityOf(Selection.AWAY));
        assertTrue(odds.oddsFor(Selection.HOME) < odds.oddsFor(Selection.AWAY));
    }

    @Test
    void theHouseMarginIsFivePercentOrMoreOnFavourites() {
        MatchOdds odds = calculator.calculate(1, TestData.team("Home", 80), TestData.team("Away", 78), null, 9);
        double overround = 1 / odds.oddsFor(Selection.HOME) + 1 / odds.oddsFor(Selection.DRAW)
                + 1 / odds.oddsFor(Selection.AWAY);
        // 5% on every selection, a little more on the favourite; rounding to 2 decimals moves it a little too
        assertTrue(overround > 1.04 && overround < 1.10, "overround " + overround);
    }

    @Test
    void noChanceIsEverZero() {
        MatchOdds odds = calculator.calculate(1, TestData.team("Giant", 95), TestData.team("Minnow", 50), null, 3);
        for (Selection selection : Selection.values()) {
            assertTrue(odds.probabilityOf(selection) > 0, selection.name());
        }
    }

    @Test
    void theSameSeedGivesTheSameOdds() {
        Team home = TestData.team("Home", 80);
        Team away = TestData.team("Away", 80);
        assertEquals(calculator.calculate(1, home, away, null, 77).odds(),
                calculator.calculate(1, home, away, null, 77).odds());
    }

    @Test
    void theSimulationsNeverTouchTheRealPlayers() {
        Team home = TestData.team("Home", 80);
        Team away = TestData.team("Away", 80);

        calculator.calculate(1, home, away, null, 11);

        for (Player player : List.of(home, away).stream().flatMap(t -> t.getSquad().stream()).toList()) {
            assertEquals(Player.MAX_CONDITION, player.getCondition());
            assertEquals(0, player.getYellowCardsAccumulated());
            assertTrue(player.isAvailable());
        }
    }

    @Test
    void aCopiedTeamHasNewPlayersWithTheSameState() {
        Team team = TestData.team("Lions", 80);
        Player original = team.getSquad().get(5);
        original.restoreState(70, 2, 1, 0);

        Player twin = TeamCopier.copy(team).getSquad().get(5);

        assertNotSame(original, twin);
        assertEquals(original.getId(), twin.getId());
        assertEquals(70, twin.getCondition());
        assertEquals(2, twin.getYellowCardsAccumulated());
        assertEquals(original.getStatus(), twin.getStatus());
    }

    @Test
    void favouritesGetABiggerMargin() {
        assertEquals(0.05, MatchOdds.marginFor(0.30, 0.05), 1e-9);    // underdog: normal margin
        assertEquals(0.05, MatchOdds.marginFor(0.50, 0.05), 1e-9);
        assertEquals(0.10, MatchOdds.marginFor(0.75, 0.05), 1e-9);    // 75% favourite: 5% extra
        assertEquals(1.21, MatchOdds.bookmakerOdds(0.75, MatchOdds.marginFor(0.75, 0.05)), 1e-9);
    }

    @Test
    void bookmakerOddsTakeTheMarginOffTheFairOdds() {
        assertEquals(1.90, MatchOdds.bookmakerOdds(0.5, 0.05), 1e-9);   // fair 2.00
        assertEquals(1.01, MatchOdds.bookmakerOdds(0.999, 0.05), 1e-9);  // never below 1.01
        MatchOdds odds = MatchOdds.fromProbabilities(3, 100, Map.of(Selection.HOME, 0.25), 0.0);
        assertEquals(4.0, odds.oddsFor(Selection.HOME), 1e-9);
    }
}
