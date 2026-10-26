package org.example.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Reads the league table calculated by the v_standings view.
public class StandingsDao {

    public record Row(String teamName, int played, int won, int drawn, int lost,
                      int goalsFor, int goalsAgainst, int goalDifference, int points) {
    }

    private static final String SELECT_BY_SEASON = """
            SELECT team_name, played, won, drawn, lost, goals_for, goals_against, goal_difference, points
            FROM v_standings
            WHERE season_id = ?
            ORDER BY points DESC, goal_difference DESC, goals_for DESC, team_name
            """;

    private final ConnectionProvider connectionProvider;

    public StandingsDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public List<Row> findBySeason(int seasonId) {
        List<Row> rows = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_SEASON)) {
            statement.setInt(1, seasonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new Row(resultSet.getString("team_name"), resultSet.getInt("played"),
                            resultSet.getInt("won"), resultSet.getInt("drawn"), resultSet.getInt("lost"),
                            resultSet.getInt("goals_for"), resultSet.getInt("goals_against"),
                            resultSet.getInt("goal_difference"), resultSet.getInt("points")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the table of season " + seasonId, e);
        }
        return rows;
    }
}
