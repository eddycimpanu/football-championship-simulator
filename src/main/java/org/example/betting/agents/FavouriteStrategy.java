package org.example.betting.agents;

import org.example.betting.Pick;
import org.example.betting.Selection;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

// The Favourite: one single on the biggest favourite of the round, 5% of the balance.
// Wins often, but the bookmaker's margin is in every price: in the long run it should lose slowly.
public class FavouriteStrategy implements BettingStrategy {

    private static final double STAKE_SHARE = 0.05;
    private static final List<Selection> RESULT = List.of(Selection.HOME, Selection.DRAW, Selection.AWAY);

    @Override
    public String description() {
        return "One single on the biggest favourite of the round, 5% of the balance";
    }

    @Override
    public List<TicketPlan> decide(List<MatchView> matches, BigDecimal balance) {
        BigDecimal stake = Stakes.share(balance, STAKE_SHARE);
        if (stake == null) {
            return List.of();
        }
        return matches.stream()
                .flatMap(m -> RESULT.stream().map(s -> new Option(m.matchId(), s, m.odds().oddsFor(s))))
                .min(Comparator.comparingDouble(Option::odds))
                .map(best -> List.of(new TicketPlan(List.of(new Pick(best.matchId(), best.selection())), stake)))
                .orElse(List.of());
    }

    private record Option(int matchId, Selection selection, double odds) {
    }
}
