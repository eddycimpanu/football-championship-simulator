package org.example.league;

import org.example.TestData;
import org.example.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeagueTableTest {

    private Team alpha;
    private Team bravo;
    private Team charlie;
    private Team delta;
    private LeagueTable table;

    @BeforeEach
    void createTable() {
        alpha = TestData.team("Alpha", 75);
        bravo = TestData.team("Bravo", 75);
        charlie = TestData.team("Charlie", 75);
        delta = TestData.team("Delta", 75);
        table = new LeagueTable(List.of(alpha, bravo, charlie, delta));
    }

    @Test
    void aWinIsThreePointsAndADrawIsOne() {
        table.record(alpha, bravo, 2, 0);
        table.record(charlie, delta, 1, 1);

        assertEquals(3, table.rowOf(alpha).getPoints());
        assertEquals(0, table.rowOf(bravo).getPoints());
        assertEquals(1, table.rowOf(charlie).getPoints());
        assertEquals(1, table.rowOf(delta).getPoints());
    }

    @Test
    void goalsAndGoalDifferenceAreCounted() {
        table.record(alpha, bravo, 3, 1);
        table.record(bravo, alpha, 2, 2);

        StandingRow row = table.rowOf(alpha);
        assertEquals(2, row.getPlayed());
        assertEquals(5, row.getGoalsFor());
        assertEquals(3, row.getGoalsAgainst());
        assertEquals(2, row.getGoalDifference());
    }

    @Test
    void pointsDecideFirst() {
        table.record(alpha, bravo, 1, 0);   // Alpha 3 points
        table.record(charlie, delta, 5, 5); // Charlie 1 point, many goals

        assertEquals(alpha, table.getLeader().getTeam());
    }

    @Test
    void levelOnPointsTheGoalDifferenceDecides() {
        table.record(alpha, charlie, 1, 0);
        table.record(bravo, delta, 4, 0);

        assertEquals(1, table.positionOf(bravo));
        assertEquals(2, table.positionOf(alpha));
    }

    @Test
    void levelOnGoalDifferenceTheGoalsScoredDecide() {
        table.record(alpha, charlie, 1, 0);
        table.record(bravo, delta, 3, 2);

        assertEquals(1, table.positionOf(bravo));
    }

    @Test
    void levelOnEverythingTheHeadToHeadDecides() {
        // Alpha and Bravo end with 3 points, goals 2-2 each. Bravo beat Alpha, so Bravo is above.
        table.record(bravo, alpha, 1, 0);
        table.record(alpha, charlie, 2, 1);
        table.record(bravo, delta, 1, 2);

        assertEquals(table.rowOf(alpha).getPoints(), table.rowOf(bravo).getPoints());
        assertEquals(table.rowOf(alpha).getGoalDifference(), table.rowOf(bravo).getGoalDifference());
        assertEquals(table.rowOf(alpha).getGoalsFor(), table.rowOf(bravo).getGoalsFor());
        assertEquals(2, table.positionOf(bravo));
        assertEquals(3, table.positionOf(alpha));
    }

    @Test
    void completelyLevelTeamsAreSortedByName() {
        assertEquals(List.of(alpha, bravo, charlie, delta),
                table.getStandings().stream().map(StandingRow::getTeam).toList());
    }

    @Test
    void theFormShowsTheResultsInOrder() {
        table.record(alpha, bravo, 1, 0);
        table.record(charlie, alpha, 1, 1);
        table.record(alpha, delta, 0, 2);

        assertEquals("WDL", table.rowOf(alpha).getForm());
    }

    @Test
    void aTeamOutsideTheLeagueIsRejected() {
        Team stranger = TestData.team("Stranger", 75);
        assertThrows(IllegalArgumentException.class, () -> table.rowOf(stranger));
    }
}
