package org.example.web;

import org.example.betting.Selection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetControllerTest {

    @Test
    void resettingStartsANewSeasonAndGivesEveryoneTheirCreditsBack() {
        WebTestKit kit = new WebTestKit();
        int match = kit.seasons.odds(1).matches().get(0).matchId();
        kit.betting.place(new BettingController.PlaceTicketRequest(new BigDecimal("100"),
                List.of(new BettingController.PickRequest(match, Selection.DRAW.name()))));
        for (int i = 0; i < 4; i++) {
            kit.seasons.playNextRound();
        }
        int oldSeason = kit.seasons.season().id();

        ResetController.ResetResponse response = kit.reset.reset();

        assertNotEquals(oldSeason, response.seasonId());
        assertEquals(0, kit.seasons.season().roundsPlayed());
        assertEquals(1, kit.seasons.season().nextRound());
        assertEquals(new BigDecimal("1000.00"), kit.betting.me().bettor().balance());
        assertTrue(kit.betting.me().settled().isEmpty());
        kit.agents.agents().forEach(a -> assertEquals(new BigDecimal("1000.00"), a.bettor().balance()));
    }

    @Test
    void theNewSeasonCanBePlayedAndBetOn() {
        WebTestKit kit = new WebTestKit();
        kit.seasons.playNextRound();
        kit.reset.reset();

        assertTrue(kit.seasons.odds(1).open());
        assertEquals(1, kit.seasons.playNextRound().number());
    }
}
