package org.example.tools;

import org.example.betting.MatchOdds;
import org.example.betting.OddsCalculator;
import org.example.betting.Selection;
import org.example.config.BettingConfig;
import org.example.config.EngineConfig;
import org.example.engine.MatchResult;
import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.ArrayList;
import java.util.List;

// Checks that the odds tell the truth: a whole season is played in memory (nothing is saved),
// the odds are calculated before every match, and the predicted chances are compared with what happened.
// If the odds are right, the selections given 60% should win about 60% of the time.
// Run: org.example.tools.OddsCalibrationCheck [simulations per match, default 300]
public class OddsCalibrationCheck {

    private static final int BUCKETS = 10;

    private record Prediction(double chance, boolean happened) {
    }

    public static void main(String[] args) {
        int simulations = args.length > 0 ? Integer.parseInt(args[0]) : 300;
        LeagueLoader loader = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load()));
        Season season = Season.create("Odds check", loader.loadTeams(), loader.loadReferees(), 2026);
        OddsCalculator calculator = new OddsCalculator(simulations, BettingConfig.ODDS_MARGIN);

        List<Prediction> predictions = new ArrayList<>();
        for (Round round : season.getRounds()) {
            List<MatchOdds> odds = round.getFixtures().parallelStream()
                    .map(f -> calculator.calculate(f.getId(), f.getHomeTeam(), f.getAwayTeam(), f.getReferee(),
                            1000L * round.getNumber() + 31L * f.getHomeTeam().getId() + f.getAwayTeam().getId()))
                    .toList();
            for (int i = 0; i < round.getFixtures().size(); i++) {
                Fixture fixture = round.getFixtures().get(i);
                MatchResult result = fixture.play(season.getRandom());
                for (Selection selection : Selection.values()) {
                    predictions.add(new Prediction(odds.get(i).probabilityOf(selection),
                            selection.wins(result.getScore().home(), result.getScore().away())));
                }
            }
            season.getTeams().forEach(t -> t.getSquad()
                    .forEach(p -> p.recoverCondition(EngineConfig.CONDITION_RECOVERY_PER_ROUND)));
            System.out.print("\rRound " + round.getNumber() + " of " + season.getNumberOfRounds());
        }
        System.out.println();
        print(predictions, simulations);
    }

    private static void print(List<Prediction> predictions, int simulations) {
        System.out.printf("%d matches, %d predictions, %d simulations per match%n%n",
                predictions.size() / Selection.values().length, predictions.size(), simulations);
        System.out.printf("%-12s %8s %10s %10s%n", "Predicted", "Count", "Expected", "Happened");
        for (int b = 0; b < BUCKETS; b++) {
            double low = (double) b / BUCKETS;
            double high = (double) (b + 1) / BUCKETS;
            List<Prediction> inBucket = predictions.stream()
                    .filter(p -> p.chance() >= low && p.chance() < high).toList();
            if (inBucket.isEmpty()) {
                continue;
            }
            double expected = inBucket.stream().mapToDouble(Prediction::chance).average().orElse(0);
            double happened = inBucket.stream().filter(Prediction::happened).count() / (double) inBucket.size();
            System.out.printf("%3.0f%%-%3.0f%%   %8d %9.1f%% %9.1f%%%n", low * 100, high * 100, inBucket.size(),
                    expected * 100, happened * 100);
        }
        // Brier score: the average squared error of the predictions (0 = perfect, lower is better)
        double brier = predictions.stream()
                .mapToDouble(p -> Math.pow(p.chance() - (p.happened() ? 1 : 0), 2)).average().orElse(0);
        System.out.printf("%nBrier score %.4f%n", brier);
    }
}
