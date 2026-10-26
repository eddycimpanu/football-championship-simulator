package org.example.persistence;

import org.example.engine.Score;
import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.SavedMatch;
import org.example.league.Season;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MatchDao {

    private static final String INSERT_FIXTURE =
            "INSERT INTO matches (season_id, round, home_team_id, away_team_id, referee_id, status) VALUES (?, ?, ?, ?, ?, 'SCHEDULED')";
    private static final String SAVE_RESULT =
            "UPDATE matches SET home_goals = ?, away_goals = ?, status = 'PLAYED' WHERE id = ?";

    private static final String SELECT_BY_SEASON = """
            SELECT id, round, home_team_id, away_team_id, referee_id, home_goals, away_goals, status
            FROM matches
            WHERE season_id = ?
            ORDER BY round, id
            """;

    // All 380 matches in one batch; each fixture receives the id the database gave it.
    public void insertSchedule(Connection connection, Season season) throws SQLException {
        List<Fixture> fixtures = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(INSERT_FIXTURE, new String[]{"id"})) {
            for (Round round : season.getRounds()) {
                for (Fixture fixture : round.getFixtures()) {
                    statement.setInt(1, season.getId());
                    statement.setInt(2, fixture.getRound());
                    statement.setInt(3, fixture.getHomeTeam().getId());
                    statement.setInt(4, fixture.getAwayTeam().getId());
                    statement.setInt(5, fixture.getReferee().getId());
                    statement.addBatch();
                    fixtures.add(fixture);
                }
            }
            statement.executeBatch();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                for (Fixture fixture : fixtures) {
                    keys.next();
                    fixture.assignId(keys.getInt(1));
                }
            }
        }
    }

    public void saveResult(Connection connection, Fixture fixture) throws SQLException {
        Score score = fixture.getScore();
        try (PreparedStatement statement = connection.prepareStatement(SAVE_RESULT)) {
            statement.setInt(1, score.home());
            statement.setInt(2, score.away());
            statement.setInt(3, fixture.getId());
            statement.executeUpdate();
        }
    }

    // The whole schedule of a season, in the order it was created, with the scores of the matches played
    public List<SavedMatch> findBySeason(Connection connection, int seasonId) throws SQLException {
        List<SavedMatch> matches = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_SEASON)) {
            statement.setInt(1, seasonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Score score = "PLAYED".equals(resultSet.getString("status"))
                            ? new Score(resultSet.getInt("home_goals"), resultSet.getInt("away_goals"))
                            : null;
                    matches.add(new SavedMatch(resultSet.getInt("id"), resultSet.getInt("round"),
                            resultSet.getInt("home_team_id"), resultSet.getInt("away_team_id"),
                            resultSet.getInt("referee_id"), score));
                }
            }
        }
        return matches;
    }
}
