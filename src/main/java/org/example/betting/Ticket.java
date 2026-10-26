package org.example.betting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// A ticket: one stake on one or more selections, at most one per match.
// It wins only if every selection wins; one wrong selection and the whole stake is lost.
public record Ticket(int id, int bettorId, int round, BigDecimal stake, BigDecimal totalOdds, BetStatus status,
                     BigDecimal payout, List<TicketSelection> selections) {

    public Ticket {
        selections = List.copyOf(selections);
    }

    // What the ticket pays if every selection wins: stake x total odds, rounded to the cent
    public BigDecimal potentialReturn() {
        return payoutFor(stake, totalOdds);
    }

    // The odds of the selections multiplied together, rounded to two decimals like a bookmaker shows them
    public static BigDecimal totalOdds(List<BigDecimal> odds) {
        return odds.stream().reduce(BigDecimal.ONE, BigDecimal::multiply).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal payoutFor(BigDecimal stake, BigDecimal totalOdds) {
        return stake.multiply(totalOdds).setScale(2, RoundingMode.HALF_UP);
    }

    // A ticket is lost as soon as one selection is lost, won when all of them are won, open otherwise
    public static BetStatus statusOf(List<BetStatus> selections) {
        if (selections.contains(BetStatus.LOST)) {
            return BetStatus.LOST;
        }
        return selections.stream().allMatch(s -> s == BetStatus.WON) ? BetStatus.WON : BetStatus.OPEN;
    }
}
