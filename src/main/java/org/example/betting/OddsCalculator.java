package org.example.betting;

import org.example.engine.Match;
import org.example.engine.MatchResult;
import org.example.engine.TeamCopier;
import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.EnumMap;
import java.util.Map;

// Monte Carlo odds: the match is played many times in memory, on copies of the two teams,
// and every selection gets the share of simulations in which it happened.
public class OddsCalculator {

    private final int simulations;
    private final double margin;

    public OddsCalculator(int simulations, double margin) {
        if (simulations < 1) {
            throw new IllegalArgumentException("At least one simulation is needed");
        }
        this.simulations = simulations;
        this.margin = margin;
    }

    public MatchOdds calculate(int matchId, Team home, Team away, Referee referee, long seed) {
        RandomProvider random = new RandomProvider(seed);
        Map<Selection, Integer> wins = new EnumMap<>(Selection.class);
        for (Selection selection : Selection.values()) {
            wins.put(selection, 0);
        }

        for (int i = 0; i < simulations; i++) {
            // Fresh copies each time: a simulated match tires players and hands out cards
            MatchResult result = new Match(TeamCopier.copy(home), TeamCopier.copy(away), referee, random).simulate();
            int homeGoals = result.getScore().home();
            int awayGoals = result.getScore().away();
            for (Selection selection : Selection.values()) {
                if (selection.wins(homeGoals, awayGoals)) {
                    wins.merge(selection, 1, Integer::sum);
                }
            }
        }

        // Laplace smoothing: one extra imaginary win for every option, so no chance is ever exactly 0%
        Map<Selection, Double> probabilities = new EnumMap<>(Selection.class);
        for (Selection selection : Selection.values()) {
            int optionsInMarket = selection.getMarket() == Selection.Market.RESULT ? 3 : 2;
            probabilities.put(selection, (wins.get(selection) + 1.0) / (simulations + optionsInMarket));
        }
        return MatchOdds.fromProbabilities(matchId, simulations, probabilities, margin);
    }
}
