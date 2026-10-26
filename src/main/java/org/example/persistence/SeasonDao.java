package org.example.persistence;

import org.example.league.Season;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SeasonDao {

    private static final String INSERT_SEASON =
            "INSERT INTO seasons (name, random_seed, current_round, status) VALUES (?, ?, 1, 'IN_PROGRESS')";
    private static final String SELECT_LATEST =
            "SELECT id, name, random_seed FROM seasons ORDER BY created_at DESC, id DESC LIMIT 1";
    private static final String UPDATE_PROGRESS =
            "UPDATE seasons SET current_round = ?, status = ? WHERE id = ?";

    // Returns the id the database gave the new season.
    public int insert(Connection connection, Season season) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SEASON, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, season.getName());
            statement.setLong(2, season.getSeed());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // The season row without its matches: what is needed to rebuild the season
    public record SeasonRow(int id, String name, long seed) {
    }

    public Optional<SeasonRow> findLatest(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_LATEST);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            return Optional.of(new SeasonRow(resultSet.getInt("id"), resultSet.getString("name"),
                    resultSet.getLong("random_seed")));
        }
    }

    public void updateProgress(Connection connection, Season season) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PROGRESS)) {
            statement.setInt(1, season.getCurrentRoundNumber());
            statement.setString(2, season.isFinished() ? "FINISHED" : "IN_PROGRESS");
            statement.setInt(3, season.getId());
            statement.executeUpdate();
        }
    }
}
