package org.example.engine;

import org.example.TestData;
import org.example.engine.event.EventType;
import org.example.engine.event.GoalEvent;
import org.example.engine.event.MatchEvent;
import org.example.engine.event.SubstitutionEvent;
import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {

    private static final Referee REFEREE = TestData.referees(1).get(0);
    private static final int SEEDS = 40;

    private static MatchResult play(int homeRating, int awayRating, long seed) {
        Team home = TestData.team("Home", homeRating);
        Team away = TestData.team("Away", awayRating);
        return new Match(home, away, REFEREE, new RandomProvider(seed)).simulate();
    }

    @Test
    void theSameSeedGivesExactlyTheSameMatch() {
        MatchResult first = play(80, 80, 12345);
        MatchResult second = play(80, 80, 12345);

        // The teams are new objects (other player ids), so compare what happened and when
        assertEquals(first.getScore(), second.getScore());
        assertEquals(first.getEvents().stream().map(event -> event.getMinute() + " " + event.getType()).toList(),
                second.getEvents().stream().map(event -> event.getMinute() + " " + event.getType()).toList());
    }

    @Test
    void theScoreAgreesWithTheGoalEventsAndTheStatistics() {
        for (long seed = 1; seed <= SEEDS; seed++) {
            MatchResult result = play(80, 78, seed);
            List<GoalEvent> goals = result.getEventsOfType(GoalEvent.class);

            long homeGoals = goals.stream().filter(goal -> goal.getTeam() == result.getHomeTeam()).count();
            long awayGoals = goals.stream().filter(goal -> goal.getTeam() == result.getAwayTeam()).count();
            assertEquals(result.getScore().home(), homeGoals, "seed " + seed);
            assertEquals(result.getScore().away(), awayGoals, "seed " + seed);
            assertEquals(result.getScore().home(), result.getHomeStats().goals(), "seed " + seed);
            assertEquals(result.getScore().away(), result.getAwayStats().goals(), "seed " + seed);
        }
    }

    @Test
    void theMatchStartsWithTheKickOffAndEventsAreInOrder() {
        for (long seed = 1; seed <= SEEDS; seed++) {
            List<MatchEvent> events = play(80, 80, seed).getEvents();

            assertEquals(EventType.KICK_OFF, events.get(0).getType());
            assertEquals(EventType.FULL_TIME, events.get(events.size() - 1).getType());
            for (int i = 1; i < events.size(); i++) {
                assertTrue(events.get(i - 1).getMinute().compareTo(events.get(i).getMinute()) <= 0,
                        "seed " + seed + ": " + events.get(i - 1) + " comes after " + events.get(i));
            }
        }
    }

    @Test
    void aTeamMakesAtMostFiveSubstitutions() {
        for (long seed = 1; seed <= SEEDS; seed++) {
            MatchResult result = play(80, 80, seed);
            for (Team team : List.of(result.getHomeTeam(), result.getAwayTeam())) {
                long substitutions = result.getEventsOfType(SubstitutionEvent.class).stream()
                        .filter(sub -> sub.getTeam() == team)
                        .count();
                assertTrue(substitutions <= 5, team.getName() + " made " + substitutions + " substitutions, seed " + seed);
            }
        }
    }

    @Test
    void nobodyPlaysMoreThanNinetyMinutes() {
        for (long seed = 1; seed <= SEEDS; seed++) {
            for (PlayerMatchStats stats : play(80, 80, seed).getAllPlayerStats()) {
                assertTrue(stats.getMinutesPlayed() >= 0 && stats.getMinutesPlayed() <= 90,
                        stats.getPlayer().getFullName() + " played " + stats.getMinutesPlayed());
            }
        }
    }

    @Test
    void possessionAddsUpToOneHundred() {
        MatchResult result = play(85, 75, 3);
        assertEquals(100, result.getHomeStats().possessionPercent() + result.getAwayStats().possessionPercent());
    }

    @Test
    void theStrongerTeamWinsMoreOftenButNotAlways() {
        int strongWins = 0;
        int notStrongWins = 0;
        for (long seed = 1; seed <= 200; seed++) {
            MatchResult result = play(85, 78, seed);
            if (result.isHomeWin()) {
                strongWins++;
            } else {
                notStrongWins++;
            }
        }
        assertTrue(strongWins > 120, "The stronger team won only " + strongWins + " of 200");
        assertTrue(notStrongWins > 0, "Luck must still matter");
    }
}
