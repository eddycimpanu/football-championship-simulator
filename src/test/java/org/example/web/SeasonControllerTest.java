package org.example.web;

import org.example.betting.MatchOdds;
import org.example.betting.Selection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonControllerTest {

    private WebTestKit kit;

    @BeforeEach
    void setUp() {
        kit = new WebTestKit();
    }

    @Test
    void aNewSeasonWaitsForRoundOne() {
        SeasonController.SeasonResponse season = kit.seasons.season();

        assertEquals(0, season.roundsPlayed());
        assertEquals(1, season.nextRound());
        assertEquals(38, season.totalRounds());
        assertFalse(season.finished());
    }

    @Test
    void theNextRoundHasOpenOddsForEveryMatch() {
        SeasonController.RoundOddsResponse odds = kit.seasons.odds(1);

        assertTrue(odds.open());
        assertEquals(10, odds.matches().size());
        for (MatchOdds match : odds.matches()) {
            double result = match.probabilityOf(Selection.HOME) + match.probabilityOf(Selection.DRAW)
                    + match.probabilityOf(Selection.AWAY);
            assertEquals(1.0, result, 1e-9);
            assertTrue(match.oddsFor(Selection.HOME) >= 1.01);
        }
    }

    @Test
    void theOddsOfARoundAreCalculatedOnlyOnce() {
        kit.seasons.odds(1);
        kit.seasons.odds(1);
        kit.seasons.odds(1);

        assertEquals(1, kit.oddsStore.getSaves());
    }

    @Test
    void playingARoundReturnsTheResultsAndMovesOn() {
        SeasonController.RoundResponse round = kit.seasons.playNextRound();

        assertEquals(1, round.number());
        assertTrue(round.played());
        round.matches().forEach(match -> {
            assertNotNull(match.homeGoals());
            assertNotNull(match.awayGoals());
        });
        assertEquals(2, kit.seasons.season().nextRound());
        assertFalse(kit.seasons.odds(1).open());           // played: the odds are only shown now
        assertTrue(kit.seasons.odds(2).open());
    }

    @Test
    void aRoundThatDoesNotExistIsNotFound() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> kit.seasons.round(39));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertThrows(ResponseStatusException.class, () -> kit.seasons.odds(0));
    }

    @Test
    void aFinishedSeasonCannotPlayAnotherRound() {
        kit.seasonService.playRemainingRounds();

        assertTrue(kit.seasons.season().finished());
        assertNull(kit.seasons.season().nextRound());
        ResponseStatusException error = assertThrows(ResponseStatusException.class, kit.seasons::playNextRound);
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
