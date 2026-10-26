package org.example;

import org.example.console.ConsoleInput;
import org.example.console.ConsoleMenu;
import org.example.league.SeasonService;
import org.example.league.SeasonStore;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.JdbcSeasonStore;
import org.example.persistence.LeagueLoader;
import org.example.persistence.MatchReportDao;
import org.example.persistence.PlayerStatsDao;
import org.example.persistence.SeasonReportDao;

import java.util.Scanner;
import java.util.function.Supplier;

public class Main {

    private static final Long SEED = null;   // null = a random new season; a number = replay that season

    public static void main(String[] args) {
        ConnectionProvider connectionProvider = new ConnectionProvider(DatabaseConfig.load());
        LeagueLoader loader = new LeagueLoader(connectionProvider);
        SeasonStore store = new JdbcSeasonStore(connectionProvider);

        // A new season always starts with fresh players, loaded again from the database
        Supplier<SeasonService> newSeason = () -> {
            long seed = SEED == null ? System.nanoTime() : SEED;
            return SeasonService.startNewSeason(SeasonService.DEFAULT_SEASON_NAME, loader.loadTeams(), loader.loadReferees(), seed, store);
        };

        // Continue the last saved season; only the very first run creates one
        SeasonService service = SeasonService.continueLatestSeason(loader.loadTeams(), loader.loadReferees(), store)
                .orElseGet(newSeason);
        System.out.println("Season " + service.getSeason().getName() + " (id " + service.getSeason().getId()
                + "), next round: " + (service.getSeason().isFinished() ? "none, the season is finished"
                : service.getSeason().getCurrentRoundNumber()));

        ConsoleMenu menu = new ConsoleMenu(service, newSeason, new PlayerStatsDao(connectionProvider),
                new MatchReportDao(connectionProvider), new SeasonReportDao(connectionProvider), new ConsoleInput(new Scanner(System.in)));
        menu.run();
    }
}
