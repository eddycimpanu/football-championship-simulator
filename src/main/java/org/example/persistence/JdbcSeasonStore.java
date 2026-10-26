package org.example.persistence;

import org.example.engine.MatchResult;
import org.example.league.Fixture;
import org.example.league.Season;
import org.example.league.SeasonStore;
import org.example.model.Referee;
import org.example.model.Team;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

// Saves a season in PostgreSQL. Every save is one transaction: all of it is written, or nothing.
public class JdbcSeasonStore implements SeasonStore {

    private final ConnectionProvider connectionProvider;
    private final SeasonDao seasonDao = new SeasonDao();
    private final MatchDao matchDao = new MatchDao();
    private final MatchEventDao eventDao = new MatchEventDao();
    private final MatchStatsDao statsDao = new MatchStatsDao();
    private final PlayerSeasonStateDao stateDao = new PlayerSeasonStateDao();

    public JdbcSeasonStore(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void saveNewSeason(Season season) {
        inTransaction("save the new season " + season.getName(), connection -> {
            season.assignId(seasonDao.insert(connection, season));
            matchDao.insertSchedule(connection, season);
        });
    }

    @Override
    public void saveMatch(Season season, Fixture fixture) {
        MatchResult result = fixture.getResult()
                .orElseThrow(() -> new IllegalStateException("Only a match played in this run can be saved: " + fixture));
        inTransaction("save the match " + result, connection -> {
            matchDao.saveResult(connection, fixture);
            eventDao.insertAll(connection, fixture.getId(), result.getEvents());
            statsDao.insertPlayerStats(connection, fixture.getId(), result.getAllPlayerStats());
            statsDao.insertTeamStats(connection, fixture.getId(), result.getHomeTeam(), result.getHomeStats());
            statsDao.insertTeamStats(connection, fixture.getId(), result.getAwayTeam(), result.getAwayStats());
        });
    }

    @Override
    public void saveRoundProgress(Season season, List<Team> teams) {
        inTransaction("save the progress of " + season.getName(), connection -> {
            seasonDao.updateProgress(connection, season);
            stateDao.saveAll(connection, season.getId(), teams);
        });
    }

    @Override
    public Optional<Season> loadLatestSeason(List<Team> teams, List<Referee> referees) {
        AtomicReference<Season> loaded = new AtomicReference<>();
        inTransaction("load the latest season", connection -> {
            Optional<SeasonDao.SeasonRow> row = seasonDao.findLatest(connection);
            if (row.isEmpty()) {
                return;
            }
            SeasonDao.SeasonRow saved = row.get();
            Season season = Season.restore(saved.id(), saved.name(), saved.seed(), teams, referees,
                    matchDao.findBySeason(connection, saved.id()));
            stateDao.restoreAll(connection, saved.id(), teams);
            loaded.set(season);
        });
        return Optional.ofNullable(loaded.get());
    }

    // ----- Transactions -----

    @FunctionalInterface
    private interface SqlWork {
        void run(Connection connection) throws SQLException;
    }

    private void inTransaction(String description, SqlWork work) {
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not " + description, e);
        }
    }
}
