package org.example;

import org.example.config.EngineConfig;
import org.example.console.LeagueTablePrinter;
import org.example.league.Fixture;
import org.example.league.Season;
import org.example.league.StandingRow;
import org.example.model.Player;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final String SEASON_NAME = "All-Star League";
    private static final int SEASONS_PER_RUN = 5;        // how many new seasons every time you press Run
    private static final boolean PRINT_EVERY_TABLE = true;
    private static final Long REPLAY_SEED = null;         // null = new random seasons; a seed = replay that one season

    public static void main(String[] args) {
        LeagueLoader loader = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load()));
        List<Referee> referees = loader.loadReferees();
        LeagueTablePrinter printer = new LeagueTablePrinter();

        int seasons = REPLAY_SEED == null ? SEASONS_PER_RUN : 1;
        Map<String, Integer> titles = new LinkedHashMap<>();

        for (int i = 1; i <= seasons; i++) {
            long seed = REPLAY_SEED == null ? System.nanoTime() : REPLAY_SEED;
            List<Team> teams = loader.loadTeams();   // fresh teams: no tiredness or bans from the last season

            Season season = Season.create(SEASON_NAME, teams, referees, seed);
            playWholeSeason(season, teams);

            StandingRow champion = season.getTable().getLeader();
            titles.merge(champion.getTeam().getName(), 1, Integer::sum);

            if (PRINT_EVERY_TABLE) {
                printer.print("Season " + i + " final table (seed " + seed + ")", season.getTable());
            }
            System.out.printf("Season %d: champion %s with %d points (seed %d)%n",
                    i, champion.getTeam().getName(), champion.getPoints(), seed);
        }

        System.out.println();
        System.out.println("=== Titles in this run ===");
        titles.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> System.out.printf("  %-20s %d%n", entry.getKey(), entry.getValue()));
        System.out.println("Copy a seed into REPLAY_SEED to watch that season again.");
    }

    private static void playWholeSeason(Season season, List<Team> teams) {
        while (!season.isFinished()) {
            for (Fixture fixture : season.getNextRound().orElseThrow().getFixtures()) {
                fixture.play(season.getRandom());
            }
            recoverAll(teams);
        }
    }

    private static void recoverAll(List<Team> teams) {
        for (Team team : teams) {
            for (Player player : team.getSquad()) {
                player.recoverCondition(EngineConfig.CONDITION_RECOVERY_PER_ROUND);
            }
        }
    }
}