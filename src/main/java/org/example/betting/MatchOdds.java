package org.example.betting;

import org.example.config.BettingConfig;

import java.util.EnumMap;
import java.util.Map;

// The chance of every selection (from the simulations) and the odds the bookmaker offers for it.
public record MatchOdds(int matchId, int simulations, Map<Selection, Double> probabilities,
                        Map<Selection, Double> odds) {

    public MatchOdds {
        probabilities = Map.copyOf(probabilities);
        odds = Map.copyOf(odds);
    }

    public double probabilityOf(Selection selection) {
        return probabilities.get(selection);
    }

    public double oddsFor(Selection selection) {
        return odds.get(selection);
    }

    // Builds the odds from the chances: fair odds = 1 / chance, then the house margin is taken off
    public static MatchOdds fromProbabilities(int matchId, int simulations, Map<Selection, Double> probabilities,
                                              double margin) {
        Map<Selection, Double> odds = new EnumMap<>(Selection.class);
        probabilities.forEach((selection, chance) ->
                odds.put(selection, bookmakerOdds(chance, marginFor(chance, margin))));
        return new MatchOdds(matchId, simulations, probabilities, odds);
    }

    // The surer the selection, the bigger the margin: from the normal margin at a 50% chance
    // up to normal + FAVOURITE_EXTRA_MARGIN at 100%. Betting on the big favourites is no easy profit.
    static double marginFor(double chance, double margin) {
        double favourite = Math.max(0, chance - 0.5) * 2;
        return margin + BettingConfig.FAVOURITE_EXTRA_MARGIN * favourite;
    }

    // With a 5% margin the chances of one market add up to 105%: the bookmaker keeps the difference
    static double bookmakerOdds(double chance, double margin) {
        double odds = 1 / (chance * (1 + margin));
        return Math.max(1.01, Math.round(odds * 100) / 100.0);
    }
}
