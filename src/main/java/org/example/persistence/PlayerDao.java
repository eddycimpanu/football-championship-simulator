package org.example.persistence;

import org.example.model.DetailedPosition;
import org.example.model.Player;
import org.example.model.PlayerAttributes;
import org.example.model.PreferredFoot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayerDao implements Repository<Player, Integer> {

    private static final String SELECT_PLAYERS = """
            SELECT id, first_name, last_name, nationality, age, shirt_number, position, preferred_foot,
                   pace, shooting, passing, dribbling, defending, physical,
                   heading, reflexes, handling, stamina, aggression
            FROM players
            """;

    private static final String SELECT_ALL = SELECT_PLAYERS + " ORDER BY team_id, shirt_number";
    private static final String SELECT_BY_ID = SELECT_PLAYERS + " WHERE id = ?";
    private static final String SELECT_BY_TEAM = SELECT_PLAYERS + " WHERE team_id = ? ORDER BY shirt_number";

    private final ConnectionProvider connectionProvider;

    public PlayerDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<Player> findById(Integer id) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapPlayer(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load player " + id, e);
        }
    }

    @Override
    public List<Player> findAll() {
        List<Player> players = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                players.add(mapPlayer(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load players", e);
        }
        return players;
    }

    public List<Player> findByTeamId(int teamId) {
        List<Player> players = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_TEAM)) {
            statement.setInt(1, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(mapPlayer(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load players of team " + teamId, e);
        }
        return players;
    }

    private Player mapPlayer(ResultSet resultSet) throws SQLException {
        PlayerAttributes attributes = new PlayerAttributes(
                resultSet.getInt("pace"),
                resultSet.getInt("shooting"),
                resultSet.getInt("passing"),
                resultSet.getInt("dribbling"),
                resultSet.getInt("defending"),
                resultSet.getInt("physical"),
                resultSet.getInt("heading"),
                resultSet.getInt("reflexes"),
                resultSet.getInt("handling"),
                resultSet.getInt("stamina"),
                resultSet.getInt("aggression"));

        return new Player(
                resultSet.getInt("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("nationality"),
                resultSet.getInt("age"),
                resultSet.getInt("shirt_number"),
                DetailedPosition.valueOf(resultSet.getString("position")),
                PreferredFoot.valueOf(resultSet.getString("preferred_foot")),
                attributes);
    }
}