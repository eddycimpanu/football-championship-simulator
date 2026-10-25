package org.example;

import org.example.config.EngineConfig;
import org.example.engine.Match;
import org.example.engine.MatchResult;
import org.example.engine.random.RandomProvider;
import org.example.model.Player;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.List;

public class Main {

    // ----- Fitness test: one team plays several rounds in a row -----
    private static final String TEAM = "Barcelona";
    private static final int ROUNDS = 8;
    private static final long SEED = 17;

    public static void main(String[] args) {
        LeagueLoader loader = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load()));
        List<Team> teams = loader.loadTeams();
        List<Referee> referees = loader.loadReferees();
        RandomProvider random = new RandomProvider(SEED);

        Team team = findTeam(teams, TEAM);

        for (int round = 1; round <= ROUNDS; round++) {
            Team opponent;
            do {
                opponent = random.pickOne(teams);
            } while (opponent == team);

            Match match = new Match(team, opponent, random.pickOne(referees), random);
            MatchResult result = match.simulate();

            System.out.println("=== Round " + round + ": " + result + " ===");
            printLineupWithCondition(result.getHomeLineup());

            // Between two rounds every player in the league recovers part of his condition
            for (Team any : teams) {
                for (Player player : any.getSquad()) {
                    player.recoverCondition(EngineConfig.CONDITION_RECOVERY_PER_ROUND);
                }
            }
        }
    }

    // Condition shown is after the match: how tired each starter is now
    private static void printLineupWithCondition(List<Player> lineup) {
        for (Player player : lineup) {
            System.out.printf("  %-4s %-26s %3d OVR   condition after match %3d%n",
                    player.getPosition(), player.getFullName(), player.getOverall(), player.getCondition());
        }
        System.out.println();
    }

    private static Team findTeam(List<Team> teams, String name) {
        return teams.stream()
                .filter(team -> team.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + name));
    }
}
