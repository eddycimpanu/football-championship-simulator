package org.example.betting.agents;

import org.example.betting.Pick;
import org.example.betting.Selection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The Steady Hand: every round the same thing, a double on the two safest selections of the round
// (any market, two different matches) with a fixed stake of 20 credits.
public class SteadyStrategy implements BettingStrategy {

    private static final String STAKE = "20";

    @Override
    public String description() {
        return "A double on the two safest selections of the round, always 20 credits";
    }

    @Override
    public List<TicketPlan> decide(List<MatchView> matches, BigDecimal balance) {
        BigDecimal stake = Stakes.fixed(balance, STAKE);
        if (stake == null) {
            return List.of();
        }
        List<Option> all = new ArrayList<>();
        for (MatchView m : matches) {
            for (Selection selection : Selection.values()) {
                all.add(new Option(m.matchId(), selection, m.odds().oddsFor(selection)));
            }
        }
        all.sort(Comparator.comparingDouble(Option::odds));
        List<Pick> picks = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (Option option : all) {
            if (picks.size() < 2 && used.add(option.matchId())) {
                picks.add(new Pick(option.matchId(), option.selection()));
            }
        }
        return picks.size() < 2 ? List.of() : List.of(new TicketPlan(picks, stake));
    }

    private record Option(int matchId, Selection selection, double odds) {
    }
}
