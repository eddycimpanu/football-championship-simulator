package org.example.betting.agents;

import org.example.betting.Pick;
import org.example.betting.Selection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// The Analyst: a value bettor with a model of its own. From the league table it estimates how many
// goals each team should score (Poisson). Like a real analyst it does not ignore the market: its final
// chance is half its model, half the bookmaker's (without the margin). It bets only where the odds
// still look too generous, and stakes by the Kelly criterion.
public class AnalystStrategy implements BettingStrategy {

    private static final int MIN_MATCHES_PLAYED = 6;    // too little data before that
    private static final double MODEL_WEIGHT = 0.5;     // the rest is the market's opinion
    private static final double MIN_EDGE = 0.04;        // bets only if its expected profit is 4% or more
    private static final double MAX_ODDS = 4.5;         // no long shots
    private static final double KELLY_FRACTION = 0.25;  // a quarter of Kelly: the model is not perfect
    private static final double MAX_SHARE = 0.05;
    private static final int MAX_BETS = 3;
    private static final double HOME_ADVANTAGE = 0.3;   // goals

    @Override
    public String description() {
        return "Bets only where its own model says the odds are too generous; stakes by the Kelly criterion";
    }

    @Override
    public List<TicketPlan> decide(List<MatchView> matches, BigDecimal balance) {
        List<Value> values = new ArrayList<>();
        for (MatchView m : matches) {
            if (m.home().played() < MIN_MATCHES_PLAYED || m.away().played() < MIN_MATCHES_PLAYED) {
                continue;
            }
            PoissonModel model = model(m.home(), m.away());
            Map<Selection, Double> modelChances = Map.of(Selection.HOME, model.homeWin(),
                    Selection.DRAW, model.draw(), Selection.AWAY, model.awayWin());
            double overround = 1 / m.odds().oddsFor(Selection.HOME) + 1 / m.odds().oddsFor(Selection.DRAW)
                    + 1 / m.odds().oddsFor(Selection.AWAY);
            modelChances.forEach((selection, modelChance) -> {
                double odds = m.odds().oddsFor(selection);
                double marketChance = (1 / odds) / overround;   // the bookmaker's chance, margin removed
                double chance = MODEL_WEIGHT * modelChance + (1 - MODEL_WEIGHT) * marketChance;
                double edge = chance * odds - 1;                 // expected profit for each credit staked
                if (edge >= MIN_EDGE && odds <= MAX_ODDS) {
                    values.add(new Value(m.matchId(), selection, chance, odds, edge));
                }
            });
        }
        values.sort(Comparator.comparingDouble(Value::edge).reversed());

        List<TicketPlan> plans = new ArrayList<>();
        List<Integer> used = new ArrayList<>();
        for (Value value : values) {
            if (plans.size() == MAX_BETS || used.contains(value.matchId())) {
                continue;
            }
            BigDecimal stake = Stakes.share(balance, Math.min(MAX_SHARE, KELLY_FRACTION * kelly(value.chance(), value.odds())));
            if (stake != null) {
                plans.add(new TicketPlan(List.of(new Pick(value.matchId(), value.selection())), stake));
                used.add(value.matchId());
            }
        }
        return plans;
    }

    // Expected goals: the average of both teams' scoring, split by the difference in goal difference
    static PoissonModel model(TeamForm home, TeamForm away) {
        double total = Math.max(2.0, Math.min(4.0, (home.goalsForPerGame() + home.goalsAgainstPerGame()
                + away.goalsForPerGame() + away.goalsAgainstPerGame()) / 2));
        double supremacy = (home.goalDifferencePerGame() - away.goalDifferencePerGame()) / 2 + HOME_ADVANTAGE;
        return new PoissonModel((total + supremacy) / 2, (total - supremacy) / 2);
    }

    // Kelly criterion: the share of the bankroll that grows it fastest, f = (b p - q) / b
    static double kelly(double chance, double odds) {
        double b = odds - 1;
        return Math.max(0, (b * chance - (1 - chance)) / b);
    }

    private record Value(int matchId, Selection selection, double chance, double odds, double edge) {
    }
}
