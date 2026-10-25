package org.example;

import org.example.config.EngineConfig;
import org.example.console.LeagueTablePrinter;
import org.example.league.Fixture;
import org.example.league.LeagueTable;
import org.example.league.Round;
import org.example.league.Season;
import org.example.league.StandingRow;
import org.example.model.Coach;
import org.example.model.Formation;
import org.example.model.Player;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.List;

public class Main {

    private static final String SEASON_NAME = "All-Star League 2026/27";
    private static final long SEED = 2026;

    public static void main(String[] args) {
        testHeadToHead();

        LeagueLoader loader = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load()));
        List<Team> teams = loader.loadTeams();
        List<Referee> referees = loader.loadReferees();

        // A temporary season loop: stage 23 moves it into SeasonService, with saving to the database
        Season season = Season.create(SEASON_NAME, teams, referees, SEED);
        LeagueTablePrinter printer = new LeagueTablePrinter();

        while (!season.isFinished()) {
            Round round = season.getNextRound().orElseThrow();
            for (Fixture fixture : round.getFixtures()) {
                fixture.play(season.getRandom());
            }
            recoverAll(teams);

            if (round.getNumber() == 19) {
                printer.print("Table after round 19 (half-time of the season)", season.getTable());
            }
        }

        LeagueTable table = season.getTable();
        printer.print(season.getName() + ": final table", table);
        System.out.println();
        System.out.println("Champion: " + table.getLeader().getTeam().getName()
                + " with " + table.getLeader().getPoints() + " points");
    }

    // All four teams finish on 4 points with goal difference 0.
    // Delta scored more goals; Alpha, Beta and Gamma are level on everything, so head-to-head decides.
    private static void testHeadToHead() {
        Coach coach = new Coach(1, "Test", "Coach", "Romania", 50, Formation.FOUR_FOUR_TWO);
        Team alpha = new Team(1, "Alpha", "ALP", "A", "Arena A", coach);
        Team beta = new Team(2, "Beta", "BET", "B", "Arena B", coach);
        Team gamma = new Team(3, "Gamma", "GAM", "C", "Arena C", coach);
        Team delta = new Team(4, "Delta", "DEL", "D", "Arena D", coach);

        LeagueTable table = new LeagueTable(List.of(alpha, beta, gamma, delta));
        table.record(alpha, beta, 0, 1);    // between Alpha, Beta and Gamma:
        table.record(beta, gamma, 0, 0);    //   Beta 4 pts, Alpha 3 pts, Gamma 1 pt
        table.record(gamma, alpha, 0, 1);
        table.record(delta, alpha, 1, 1);
        table.record(delta, beta, 2, 1);
        table.record(gamma, delta, 2, 1);

        System.out.println("=== Head-to-head test ===");
        for (StandingRow row : table.getStandings()) {
            System.out.printf("  %-6s %d pts, GD %+d, GF %d%n", row.getTeam().getName(),
                    row.getPoints(), row.getGoalDifference(), row.getGoalsFor());
        }
        System.out.println("  Expected: Delta (more goals), then Beta, Alpha, Gamma (head-to-head)");
        System.out.println();
    }

    private static void recoverAll(List<Team> teams) {
        for (Team team : teams) {
            for (Player player : team.getSquad()) {
                player.recoverCondition(EngineConfig.CONDITION_RECOVERY_PER_ROUND);
            }
        }
    }
}
