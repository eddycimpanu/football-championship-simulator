package org.example.betting.agents;

import org.example.betting.Pick;
import org.example.betting.Selection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// The Statistician: only the goals markets (over/under 2.5, both teams score). It expects each team to
// score the average of its attack and the opponent's defence, and bets where Poisson disagrees with the odds.
public class StatisticianStrategy implements BettingStrategy {

    private static final int MIN_MATCHES_PLAYED = 4;
    private static final double MIN_EDGE = 0.06;
    private static final double STAKE_SHARE = 0.03;
    private static final int MAX_BETS = 2;
    private static final double HOME_BOOST = 1.08;
    private static final double AWAY_DROP = 0.92;

    @Override
    public String description() {
        return "Only goals markets, from the teams' scoring averages and a Poisson model; 3% a bet";
    }

    @Override
    public List<TicketPlan> decide(List<MatchView> matches, BigDecimal balance) {
        BigDecimal stake = Stakes.share(balance, STAKE_SHARE);
        if (stake == null) {
            return List.of();
        }
        List<Option> options = new ArrayList<>();
        for (MatchView m : matches) {
            if (m.home().played() < MIN_MATCHES_PLAYED || m.away().played() < MIN_MATCHES_PLAYED) {
                continue;
            }
            double homeGoals = (m.home().goalsForPerGame() + m.away().goalsAgainstPerGame()) / 2 * HOME_BOOST;
            double awayGoals = (m.away().goalsForPerGame() + m.home().goalsAgainstPerGame()) / 2 * AWAY_DROP;
            PoissonModel model = new PoissonModel(homeGoals, awayGoals);
            Map<Selection, Double> chances = Map.of(
                    Selection.OVER_2_5, model.over25(), Selection.UNDER_2_5, 1 - model.over25(),
                    Selection.BOTH_SCORE, model.bothScore(), Selection.NOT_BOTH_SCORE, 1 - model.bothScore());
            chances.forEach((selection, chance) -> {
                double edge = chance * m.odds().oddsFor(selection) - 1;
                if (edge >= MIN_EDGE) {
                    options.add(new Option(m.matchId(), selection, edge));
                }
            });
        }
        options.sort(Comparator.comparingDouble(Option::edge).reversed());
        List<TicketPlan> plans = new ArrayList<>();
        List<Integer> used = new ArrayList<>();
        for (Option option : options) {
            if (plans.size() < MAX_BETS && !used.contains(option.matchId())) {
                plans.add(new TicketPlan(List.of(new Pick(option.matchId(), option.selection())), stake));
                used.add(option.matchId());
            }
        }
        return plans;
    }

    private record Option(int matchId, Selection selection, double edge) {
    }
}
