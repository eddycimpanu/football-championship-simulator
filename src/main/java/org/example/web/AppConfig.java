package org.example.web;

import org.example.betting.BettingService;
import org.example.betting.agents.Agent;
import org.example.betting.agents.AgentService;
import org.example.betting.BettingStore;
import org.example.betting.OddsCalculator;
import org.example.betting.OddsService;
import org.example.betting.OddsStore;
import org.example.config.BettingConfig;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.league.LeagueSource;
import org.example.league.SeasonService;
import org.example.league.SeasonStore;
import org.example.persistence.BettingDao;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.JdbcSeasonStore;
import org.example.persistence.LeagueLoader;
import org.example.persistence.MatchReportDao;
import org.example.persistence.OddsDao;
import org.example.persistence.PlayerStatsDao;
import org.example.persistence.SeasonReportDao;
import org.example.persistence.StandingsDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

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

    // Where the reset button gets fresh clubs and players: the database, through LeagueLoader
    @Bean
    public LeagueSource leagueSource(LeagueLoader loader) {
        return new LeagueSource() {
            @Override
            public List<Team> loadTeams() {
                return loader.loadTeams();
            }

            @Override
            public List<Referee> loadReferees() {
                return loader.loadReferees();
            }
        };
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

    // ----- Betting -----

    @Bean
    public OddsStore oddsStore(ConnectionProvider connectionProvider) {
        return new OddsDao(connectionProvider);
    }

    @Bean
    public OddsService oddsService(OddsStore oddsStore) {
        return new OddsService(new OddsCalculator(BettingConfig.ODDS_SIMULATIONS, BettingConfig.ODDS_MARGIN), oddsStore);
    }

    @Bean
    public BettingStore bettingStore(ConnectionProvider connectionProvider) {
        return new BettingDao(connectionProvider);
    }

    @Bean
    public BettingService bettingService(OddsService oddsService, BettingStore bettingStore) {
        return new BettingService(oddsService, bettingStore);
    }

    @Bean
    public AgentService agentService(BettingService bettingService, BettingStore bettingStore) {
        return new AgentService(bettingService, bettingStore, Agent.all());
    }
}
