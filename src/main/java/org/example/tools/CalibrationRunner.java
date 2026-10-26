package org.example.tools;

import org.example.config.EngineConfig;
import org.example.engine.MatchResult;
import org.example.engine.PlayerMatchStats;
import org.example.engine.TeamMatchStats;
import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;
import org.example.league.StandingRow;
import org.example.model.Player;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Plays many seasons in memory (nothing is saved) and prints the averages used to tune EngineConfig:
// match statistics, how spread out the table is, and how each team does compared to its strength.
// Run it after changing EngineConfig: org.example.tools.CalibrationRunner [number of seasons, default 20]
public class CalibrationRunner {

    private static final int SEASONS = 20;
    private static final long FIRST_SEED = 1_000;
    private static final int BEST_ELEVEN = 11;

    public static void main(String[] args) {
        int seasons = args.length > 0 ? Integer.parseInt(args[0]) : SEASONS;
        LeagueLoader loader = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load()));
        List<Referee> referees = loader.loadReferees();

        Totals totals = new Totals();
        for (int i = 0; i < seasons; i++) {
            // Fresh players every season: condition, cards and injuries start again
            Season season = Season.create("Calibration " + (i + 1), loader.loadTeams(), referees, FIRST_SEED + i);
            for (Round round : season.getRounds()) {
                for (Fixture fixture : round.getFixtures()) {
                    totals.addMatch(fixture.play(season.getRandom()));
                }
                season.getTeams().forEach(team -> team.getSquad()
                        .forEach(player -> player.recoverCondition(EngineConfig.CONDITION_RECOVERY_PER_ROUND)));
            }
            totals.addSeason(season);
        }
        totals.print(seasons);
    }

    private static final class Totals {
        private int matches;
        private int goals, shots, onTarget, corners, fouls, yellows, reds, assists;
        private int homeWins, draws, awayWins;
        private double ratingSum;
        private int rated;
        private double championPoints, lastPoints, pointsSpread, topScorerGoals, topAssists;
        private final Map<String, double[]> byTeam = new HashMap<>();   // points, position, team OVR
        private final Map<Player, int[]> seasonPlayer = new HashMap<>(); // goals, assists this season

        void addMatch(MatchResult result) {
            matches++;
            for (TeamMatchStats stats : List.of(result.getHomeStats(), result.getAwayStats())) {
                goals += stats.goals();
                shots += stats.shots();
                onTarget += stats.shotsOnTarget();
                corners += stats.corners();
                fouls += stats.fouls();
                yellows += stats.yellowCards();
                reds += stats.redCards();
            }
            for (PlayerMatchStats stats : result.getAllPlayerStats()) {
                assists += stats.getAssists();
                if (stats.isRated()) {
                    ratingSum += stats.getRating();
                    rated++;
                }
                int[] line = seasonPlayer.computeIfAbsent(stats.getPlayer(), p -> new int[2]);
                line[0] += stats.getGoals();
                line[1] += stats.getAssists();
            }
            if (result.isHomeWin()) homeWins++;
            else if (result.isDraw()) draws++;
            else awayWins++;
        }

        void addSeason(Season season) {
            List<StandingRow> standings = season.getTable().getStandings();
            championPoints += standings.get(0).getPoints();
            lastPoints += standings.get(standings.size() - 1).getPoints();
            double mean = standings.stream().mapToInt(StandingRow::getPoints).average().orElse(0);
            pointsSpread += Math.sqrt(standings.stream()
                    .mapToDouble(row -> Math.pow(row.getPoints() - mean, 2)).average().orElse(0));
            for (int i = 0; i < standings.size(); i++) {
                Team team = standings.get(i).getTeam();
                double[] line = byTeam.computeIfAbsent(team.getName(), name -> new double[3]);
                line[0] += standings.get(i).getPoints();
                line[1] += i + 1;
                line[2] = bestElevenOverall(team);
            }
            topScorerGoals += seasonPlayer.values().stream().mapToInt(line -> line[0]).max().orElse(0);
            topAssists += seasonPlayer.values().stream().mapToInt(line -> line[1]).max().orElse(0);
            seasonPlayer.clear();
        }

        void print(int seasons) {
            double m = matches;
            System.out.printf("%d seasons, %d matches%n", seasons, matches);
            System.out.printf("Per match: goals %.2f, shots %.1f (on target %.1f), corners %.1f, fouls %.1f, yellow %.2f, red %.2f%n",
                    goals / m, shots / m, onTarget / m, corners / m, fouls / m, yellows / m, reds / m);
            System.out.printf("Assisted goals: %.0f%%, average player rating %.2f%n", 100.0 * assists / goals, ratingSum / rated);
            System.out.printf("Home / draw / away: %.0f / %.0f / %.0f%n", 100 * homeWins / m, 100 * draws / m, 100 * awayWins / m);
            System.out.printf("Champion %.1f pts, last %.1f pts, spread (std dev) %.1f pts%n",
                    championPoints / seasons, lastPoints / seasons, pointsSpread / seasons);
            System.out.printf("Top scorer %.1f goals, top assists %.1f%n", topScorerGoals / seasons, topAssists / seasons);
            System.out.println();
            System.out.printf("%-20s %6s %6s %6s%n", "Team", "OVR XI", "Points", "Pos");
            byTeam.entrySet().stream()
                    .sorted(Comparator.comparingDouble(entry -> -entry.getValue()[2]))
                    .forEach(entry -> System.out.printf("%-20s %6.1f %6.1f %6.1f%n", entry.getKey(),
                            entry.getValue()[2], entry.getValue()[0] / seasons, entry.getValue()[1] / seasons));
        }

        private static double bestElevenOverall(Team team) {
            List<Integer> overalls = new ArrayList<>(team.getSquad().stream().map(Player::getOverall).toList());
            overalls.sort(Comparator.reverseOrder());
            return overalls.stream().limit(BEST_ELEVEN).mapToInt(Integer::intValue).average().orElse(0);
        }
    }
}
