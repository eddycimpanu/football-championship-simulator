package org.example.web;

import org.example.betting.BettorStats;
import org.example.betting.Selection;
import org.example.league.StandingRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BettorsControllerTest {

    private WebTestKit kit;

    @BeforeEach
    void setUp() {
        kit = new WebTestKit();
    }

    private void playRounds(int count) {
        for (int i = 0; i < count; i++) {
            kit.seasons.playNextRound();
        }
    }

    @Test
    void youAndTheFiveAgentsAreInTheTable() {
        BettorsController.BettorsResponse response = kit.bettors.bettors();

        assertEquals(6, response.bettors().size());
        assertEquals(1, response.bettors().stream().filter(s -> s.bettor().kind().equals("HUMAN")).count());
    }

    @Test
    void theRichestBettorIsFirst() {
        playRounds(6);

        List<BettorStats> table = kit.bettors.bettors().bettors();
        for (int i = 1; i < table.size(); i++) {
            assertTrue(table.get(i - 1).bettor().balance().compareTo(table.get(i).bettor().balance()) >= 0);
        }
    }

    @Test
    void theChartEndsAtTheRealBalance() {
        // You bet on round 1 too, so your line moves
        int match = kit.seasons.odds(1).matches().get(0).matchId();
        kit.betting.place(new BettingController.PlaceTicketRequest(new BigDecimal("50"),
                List.of(new BettingController.PickRequest(match, Selection.HOME.name()))));
        playRounds(5);

        BettorsController.BettorsResponse response = kit.bettors.bettors();
        assertEquals(5, response.roundsPlayed());
        for (BettorStats stats : response.bettors()) {
            assertEquals(6, stats.history().size());                       // the start and 5 rounds
            assertEquals(0, new BigDecimal("1000").compareTo(stats.history().get(0).balance()));
            // Nothing is waiting after a played round, so the last point is the balance
            assertEquals(0, stats.bettor().balance().compareTo(stats.history().get(5).balance()), stats.bettor().name());
            assertEquals(0, stats.profit().compareTo(stats.bettor().balance().subtract(new BigDecimal("1000"))));
        }
    }

    @Test
    void theChampionIsOnlyKnownAtTheEnd() {
        assertNull(kit.bettors.bettors().champion());

        kit.seasonService.playRemainingRounds();

        BettorsController.BettorsResponse response = kit.bettors.bettors();
        StandingRow leader = kit.seasonService.getTable().getLeader();
        assertTrue(response.finished());
        assertEquals(leader.getTeam().getName(), response.champion().team());
        assertEquals(leader.getPoints(), response.champion().points());
    }

    @Test
    void theTicketHistoryOfABettor() {
        playRounds(3);
        BettorStats steady = kit.bettors.bettors().bettors().stream()
                .filter(s -> "STEADY".equals(s.bettor().strategy())).findFirst().orElseThrow();

        assertEquals(3, kit.bettors.tickets(steady.bettor().id()).size());
        assertThrows(ResponseStatusException.class, () -> kit.bettors.tickets(9999));
    }
}
