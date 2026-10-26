package org.example.betting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BettorStatsTest {

    private static Ticket ticket(int round, String stake, String odds, BetStatus status, String payout) {
        return new Ticket(round, 1, round, new BigDecimal(stake), new BigDecimal(odds), status,
                payout == null ? null : new BigDecimal(payout), List.of());
    }

    @Test
    void theBalanceIsFollowedRoundByRound() {
        Bettor bettor = new Bettor(1, 1, "You", "HUMAN", null, new BigDecimal("1000.00"), new BigDecimal("1060.00"));
        List<Ticket> tickets = List.of(
                ticket(1, "50.00", "2.00", BetStatus.WON, "100.00"),   // +50 after round 1
                ticket(2, "30.00", "3.00", BetStatus.LOST, "0.00"),    // -30 after round 2
                ticket(3, "40.00", "1.50", BetStatus.OPEN, null));     // round 3 not played yet

        BettorStats stats = BettorStats.of(bettor, "You", tickets, 2);

        assertEquals(3, stats.history().size());
        assertEquals(new BigDecimal("1000.00"), stats.history().get(0).balance());
        assertEquals(new BigDecimal("1050.00"), stats.history().get(1).balance());
        assertEquals(new BigDecimal("1020.00"), stats.history().get(2).balance());
        assertEquals(1, stats.won());
        assertEquals(1, stats.lost());
        assertEquals(1, stats.open());
    }

    @Test
    void returnOnStakesAndBiggestWin() {
        Bettor bettor = new Bettor(1, 1, "You", "HUMAN", null, new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        List<Ticket> tickets = List.of(
                ticket(1, "50.00", "2.00", BetStatus.WON, "100.00"),
                ticket(1, "10.00", "8.00", BetStatus.WON, "80.00"),
                ticket(2, "40.00", "3.00", BetStatus.LOST, "0.00"));

        BettorStats stats = BettorStats.of(bettor, "You", tickets, 2);

        assertEquals(0.8, stats.returnOnStakes(), 1e-9);      // staked 100, got back 180
        assertEquals(2.0 / 3, stats.winRate(), 1e-9);
        assertEquals(new BigDecimal("100.00"), stats.biggestWin());
    }

    @Test
    void noTicketsMeansNoRates() {
        Bettor bettor = new Bettor(1, 1, "You", "HUMAN", null, new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        BettorStats stats = BettorStats.of(bettor, "You", List.of(), 5);
        assertNull(stats.returnOnStakes());
        assertNull(stats.winRate());
        assertEquals(6, stats.history().size());
    }
}
