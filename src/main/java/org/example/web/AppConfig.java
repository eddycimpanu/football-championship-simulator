package org.example.web;

import org.example.league.SeasonService;
import org.example.league.SeasonStore;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.JdbcSeasonStore;
import org.example.persistence.LeagueLoader;
import org.example.persistence.MatchReportDao;
import org.example.persistence.PlayerStatsDao;
import org.example.persistence.SeasonReportDao;
import org.example.persistence.StandingsDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

// Creates the objects the web application needs, once, and lets Spring inject them where needed.
// The classes themselves stay free of Spring: the console builds the same objects by hand in Main.
@Configuration
public class AppConfig {

    // The pool (HikariCP) is created by Spring Boot from the spring.datasource settings
    @Bean
    public ConnectionProvider connectionProvider(DataSource dataSource) {
        return new ConnectionProvider(dataSource);
    }

    @Bean
    public LeagueLoader leagueLoader(ConnectionProvider connectionProvider) {
        return new LeagueLoader(connectionProvider);
    }

    @Bean
    public SeasonStore seasonStore(ConnectionProvider connectionProvider) {
        return new JdbcSeasonStore(connectionProvider);
    }

    // Same rule as the console: continue the last saved season, create one only if there is none
    @Bean
    public SeasonService seasonService(LeagueLoader loader, SeasonStore store) {
        return SeasonService.continueLatestSeason(loader.loadTeams(), loader.loadReferees(), store)
                .orElseGet(() -> SeasonService.startNewSeason(SeasonService.DEFAULT_SEASON_NAME,
                        loader.loadTeams(), loader.loadReferees(), System.nanoTime(), store));
    }

    // The read-only DAOs used by the next stages (table, matches, players, reports)

    @Bean
    public StandingsDao standingsDao(ConnectionProvider connectionProvider) {
        return new StandingsDao(connectionProvider);
    }

    @Bean
    public PlayerStatsDao playerStatsDao(ConnectionProvider connectionProvider) {
        return new PlayerStatsDao(connectionProvider);
    }

    @Bean
    public MatchReportDao matchReportDao(ConnectionProvider connectionProvider) {
        return new MatchReportDao(connectionProvider);
    }

    @Bean
    public SeasonReportDao seasonReportDao(ConnectionProvider connectionProvider) {
        return new SeasonReportDao(connectionProvider);
    }
}
