package org.example.league;

import org.example.TestData;
import org.example.model.Referee;
import org.example.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonServiceTest {

    private List<Team> teams;
    private List<Referee> referees;
    private InMemorySeasonStore store;
    private SeasonService service;

    @BeforeEach
    void startSeason() {
        teams = TestData.league(20, 78);
        referees = TestData.referees(12);
        store = new InMemorySeasonStore();
        service = SeasonService.startNewSeason("Test League", teams, referees, 99, store);
    }

    @Test
    void aNewSeasonIsSavedWithItsWholeSchedule() {
        assertEquals(1, service.getSeason().getId());
        assertEquals(380, store.getSchedule().size());
        assertTrue(store.getSchedule().stream().noneMatch(SavedMatch::isPlayed));
    }

    @Test
    void playingARoundSavesItsTenMatchesAndTheProgress() {
        Round round = service.playNextRound();

        assertEquals(1, round.getNumber());
        assertTrue(round.isPlayed());
        assertEquals(10, store.matchesSaved);
        assertEquals(1, store.progressSaves);
        assertEquals(2, service.getSeason().getCurrentRoundNumber());
    }

    @Test
    void aFinishedSeasonCannotPlayAnotherRound() {
        service.playRemainingRounds();

        assertTrue(service.getSeason().isFinished());
        assertEquals(380, store.matchesSaved);
        service.getTable().getStandings().forEach(row -> assertEquals(38, row.getPlayed()));
        assertThrows(IllegalStateException.class, service::playNextRound);
    }

    @Test
    void restartingStartsTheLeagueAgainFromRoundOne() {
        service.playNextRound();
        service.playNextRound();
        Season old = service.getSeason();

        service.restart(TestData.league(20, 78), referees, 7);

        assertNotSame(old, service.getSeason());
        assertEquals(2, service.getSeason().getId());
        assertEquals(380, store.getSchedule().size());          // the new schedule was saved
        assertEquals("Test League", service.getSeason().getName());
        assertEquals(0, service.getSeason().getRoundsPlayed());
        assertEquals(1, service.getSeason().getCurrentRoundNumber());
    }

    @Test
    void aContinuedSeasonStartsWhereItStopped() {
        for (int i = 0; i < 3; i++) {
            service.playNextRound();
        }
        LeagueTable before = service.getTable();

        SeasonService continued = SeasonService.continueLatestSeason(teams, referees, store).orElseThrow();

        assertEquals(4, continued.getSeason().getCurrentRoundNumber());
        for (Team team : teams) {
            assertEquals(before.rowOf(team).getPoints(), continued.getTable().rowOf(team).getPoints(), team.getName());
            assertEquals(before.rowOf(team).getGoalDifference(), continued.getTable().rowOf(team).getGoalDifference());
        }
    }

    @Test
    void aRoundStoppedHalfwayOnlyPlaysTheMissingMatches() {
        service.playNextRound();
        // The program "stopped" after the first 4 matches of round 1 were saved
        List<SavedMatch> roundOne = store.getSchedule().stream().filter(match -> match.round() == 1).toList();
        roundOne.subList(4, 10).forEach(match -> store.forgetResult(match.id()));
        store.matchesSaved = 0;

        SeasonService continued = SeasonService.continueLatestSeason(teams, referees, store).orElseThrow();
        assertEquals(1, continued.getSeason().getCurrentRoundNumber());
        continued.playNextRound();

        assertEquals(6, store.matchesSaved);
        assertEquals(2, continued.getSeason().getCurrentRoundNumber());
    }

    @Test
    void nothingToContinueWhenNoSeasonIsSaved() {
        assertTrue(SeasonService.continueLatestSeason(teams, referees, new InMemorySeasonStore()).isEmpty());
    }
}
