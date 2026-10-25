package org.example;

import org.example.config.EngineConfig;
import org.example.console.MatchReportPrinter;
import org.example.engine.Match;
import org.example.engine.MatchResult;
import org.example.engine.event.CardEvent;
import org.example.engine.event.CoachCardEvent;
import org.example.engine.event.EventDisplayMode;
import org.example.engine.random.RandomProvider;
import org.example.model.Player;
import org.example.model.PlayerStatus;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final long SEED = 18;
    private static final int TEST_ROUNDS = 10;

    public static void main(String[] args) {
        LeagueLoader loader = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load()));
        List<Team> teams = loader.loadTeams();
        List<Referee> referees = loader.loadReferees();
        RandomProvider random = new RandomProvider(SEED);

        // 1. One match with the key events: goals and cards
        MatchResult classic = new Match(findTeam(teams, "Real Madrid"), findTeam(teams, "Atlético Madrid"),
                random.pickOne(referees), random).simulate();
        MatchReportPrinter printer = new MatchReportPrinter();
        printer.printEvents(classic, EventDisplayMode.KEY);
        printer.printStats(classic);

        // 2. Several rounds with all 20 teams, to see cards and bans over time
        System.out.println();
        System.out.println("=== " + TEST_ROUNDS + " rounds, 10 matches each ===");
        int matches = 0, yellows = 0, reds = 0, coachCards = 0;

        for (int round = 1; round <= TEST_ROUNDS; round++) {
            printUnavailable(round, teams);

            List<Team> left = new ArrayList<>(teams);
            while (!left.isEmpty()) {
                Team home = left.remove(random.nextInt(0, left.size() - 1));
                Team away = left.remove(random.nextInt(0, left.size() - 1));
                MatchResult result = new Match(home, away, random.pickOne(referees), random).simulate();

                matches++;
                yellows += result.getHomeStats().yellowCards() + result.getAwayStats().yellowCards();
                reds += result.getHomeStats().redCards() + result.getAwayStats().redCards();
                coachCards += result.getEventsOfType(CoachCardEvent.class).size();
                for (CardEvent card : result.getEventsOfType(CardEvent.class)) {
                    if (card.isSendingOff()) {
                        System.out.println("  " + card.getMinute() + " " + card.describe() + "   in " + result);
                    }
                }
            }
            recoverAll(teams);
        }

        System.out.println();
        System.out.printf("Yellow cards per match: %.2f   (real football: 3.5 - 4.5)%n", (double) yellows / matches);
        System.out.printf("Red cards per match:    %.2f   (real football: 0.1 - 0.2)%n", (double) reds / matches);
        System.out.printf("Coach cards per match:  %.2f%n", (double) coachCards / matches);
    }

    private static void printUnavailable(int round, List<Team> teams) {
        System.out.println("Round " + round + ":");
        for (Team team : teams) {
            for (Player player : team.getSquad()) {
                if (player.getStatus() == PlayerStatus.SUSPENDED) {
                    System.out.println("  SUSPENDED: " + player.getFullName() + " (" + team.getName() + ")");
                }
            }
        }
    }

    private static void recoverAll(List<Team> teams) {
        for (Team team : teams) {
            for (Player player : team.getSquad()) {
                player.recoverCondition(EngineConfig.CONDITION_RECOVERY_PER_ROUND);
            }
        }
    }

    private static Team findTeam(List<Team> teams, String name) {
        return teams.stream()
                .filter(team -> team.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + name));
    }
}
