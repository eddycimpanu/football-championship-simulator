package org.example.league;

import org.example.TestData;
import org.example.engine.Score;
import org.example.model.Referee;
import org.example.model.Team;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonTest {

    private final List<Team> teams = TestData.league(20, 75);
    private final List<Referee> referees = TestData.referees(12);

    @Test
    void everyMatchHasAReferee() {
        Season season = Season.create("Test", teams, referees, 1);
        for (Round round : season.getRounds()) {
            Set<Referee> busy = new HashSet<>();
            for (Fixture fixture : round.getFixtures()) {
                assertNotNull(fixture.getReferee());
                assertTrue(busy.add(fixture.getReferee()), "A referee has two matches in " + round);
            }
        }
    }

    @Test
    void aNewSeasonStartsAtRoundOneWithAnEmptyTable() {
        Season season = Season.create("Test", teams, referees, 1);

        assertEquals(1, season.getCurrentRoundNumber());
        assertFalse(season.isFinished());
        season.getTable().getStandings().forEach(row -> assertEquals(0, row.getPlayed()));
    }

    @Test
    void restoreRebuildsTheScheduleAndTheScores() {
        List<SavedMatch> schedule = List.of(
                new SavedMatch(1, 1, teams.get(0).getId(), teams.get(1).getId(), referees.get(0).getId(), new Score(2, 1)),
                new SavedMatch(2, 1, teams.get(2).getId(), teams.get(3).getId(), referees.get(1).getId(), null),
                new SavedMatch(3, 2, teams.get(1).getId(), teams.get(2).getId(), referees.get(2).getId(), null));

        Season season = Season.restore(7, "Saved", 5, teams, referees, schedule);

        assertEquals(7, season.getId());
        assertEquals(2, season.getNumberOfRounds());
        Fixture played = season.getRound(1).getFixtures().get(0);
        assertTrue(played.isPlayed());
        assertEquals(new Score(2, 1), played.getScore());
        assertTrue(played.getResult().isEmpty());          // only the score comes back, not the events
        assertEquals(3, season.getTable().rowOf(teams.get(0)).getPoints());
        assertEquals(1, season.getCurrentRoundNumber());   // round 1 still has a match to play
    }

    @Test
    void restoreRejectsATeamThatDoesNotExist() {
        List<SavedMatch> schedule = List.of(new SavedMatch(1, 1, -5, teams.get(1).getId(), referees.get(0).getId(), null));
        assertThrows(IllegalStateException.class, () -> Season.restore(1, "Broken", 5, teams, referees, schedule));
    }

    @Test
    void roundNumbersOutsideTheSeasonAreRejected() {
        Season season = Season.create("Test", teams, referees, 1);
        assertThrows(IllegalArgumentException.class, () -> season.getRound(0));
        assertThrows(IllegalArgumentException.class, () -> season.getRound(39));
    }
}
