package org.example.betting.agents;

import org.example.betting.Pick;
import org.example.betting.Selection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The Dreamer: one long ticket of four upsets at big odds, with a small stake.
// Almost always loses, but one win pays for a whole season.
public class DreamerStrategy implements BettingStrategy {

    private static final double STAKE_SHARE = 0.02;
    private static final int SELECTIONS = 4;
    private static final double MIN_ODDS = 2.6;
    private static final double MAX_ODDS = 6.0;

    @Override
    public String description() {
        return "A four-match ticket of upsets at big odds, 2% of the balance";
    }

    @Override
    public List<TicketPlan> decide(List<MatchView> matches, BigDecimal balance) {
        BigDecimal stake = Stakes.share(balance, STAKE_SHARE);
        if (stake == null) {
            return List.of();
        }
        // The underdog of every match (the bigger of the two win odds), if its odds are in range
        List<Option> underdogs = new ArrayList<>();
        for (MatchView m : matches) {
            double home = m.odds().oddsFor(Selection.HOME);
            double away = m.odds().oddsFor(Selection.AWAY);
            Option underdog = home > away ? new Option(m.matchId(), Selection.HOME, home)
                    : new Option(m.matchId(), Selection.AWAY, away);
            if (underdog.odds() >= MIN_ODDS && underdog.odds() <= MAX_ODDS) {
                underdogs.add(underdog);
            }
        }
        // The least unlikely upsets first: the dream should at least be possible
        underdogs.sort(Comparator.comparingDouble(Option::odds));
        List<Pick> picks = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (Option option : underdogs) {
            if (picks.size() == SELECTIONS) {
                break;
            }
            if (used.add(option.matchId())) {
                picks.add(new Pick(option.matchId(), option.selection()));
            }
        }
        return picks.size() < SELECTIONS ? List.of() : List.of(new TicketPlan(picks, stake));
    }

    private record Option(int matchId, Selection selection, double odds) {
    }
}
