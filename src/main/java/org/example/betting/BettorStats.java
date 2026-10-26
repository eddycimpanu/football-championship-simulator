package org.example.betting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// One line of the bettors table, with the balance after every round for the chart.
public record BettorStats(Bettor bettor, String description, int won, int lost, int open, BigDecimal staked,
                          BigDecimal returned, BigDecimal profit, Double returnOnStakes, Double winRate,
                          BigDecimal biggestWin, BigDecimal biggestWinOdds, List<BalancePoint> history) {

    public record BalancePoint(int round, BigDecimal balance) {
    }

    public BettorStats {
        history = List.copyOf(history);
    }

    // The history starts at round 0 with the starting credits. A ticket's stake leaves the balance
    // in the round it was placed for, and its payout comes back at the end of that round.
    public static BettorStats of(Bettor bettor, String description, List<Ticket> tickets, int roundsPlayed) {
        int won = 0;
        int lost = 0;
        int open = 0;
        BigDecimal staked = BigDecimal.ZERO;
        BigDecimal returned = BigDecimal.ZERO;
        BigDecimal biggestWin = null;
        BigDecimal biggestWinOdds = null;
        for (Ticket ticket : tickets) {
            switch (ticket.status()) {
                case WON -> {
                    won++;
                    staked = staked.add(ticket.stake());
                    returned = returned.add(ticket.payout());
                    if (biggestWin == null || ticket.payout().compareTo(biggestWin) > 0) {
                        biggestWin = ticket.payout();
                        biggestWinOdds = ticket.totalOdds();
                    }
                }
                case LOST -> {
                    lost++;
                    staked = staked.add(ticket.stake());
                }
                case OPEN -> open++;
            }
        }

        List<BalancePoint> history = new ArrayList<>();
        BigDecimal balance = bettor.startingBalance();
        history.add(new BalancePoint(0, balance));
        for (int round = 1; round <= roundsPlayed; round++) {
            for (Ticket ticket : tickets) {
                if (ticket.round() == round && ticket.status() != BetStatus.OPEN) {
                    balance = balance.subtract(ticket.stake());
                    if (ticket.status() == BetStatus.WON) {
                        balance = balance.add(ticket.payout());
                    }
                }
            }
            history.add(new BalancePoint(round, balance));
        }

        Double returnOnStakes = staked.signum() == 0 ? null
                : returned.subtract(staked).divide(staked, 4, RoundingMode.HALF_UP).doubleValue();
        Double winRate = won + lost == 0 ? null : (double) won / (won + lost);
        return new BettorStats(bettor, description, won, lost, open, staked, returned, bettor.profit(),
                returnOnStakes, winRate, biggestWin, biggestWinOdds, history);
    }
}
