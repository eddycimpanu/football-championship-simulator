package org.example.persistence;

import org.example.model.Coach;
import org.example.model.Formation;
import org.example.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeamDao implements Repository<Team, Integer> {

    private static final String SELECT_TEAMS_WITH_COACH = """
            SELECT t.id, t.name, t.short_name, t.city, t.stadium,
                   c.id          AS coach_id,
                   c.first_name  AS coach_first_name,
                   c.last_name   AS coach_last_name,
                   c.nationality AS coach_nationality,
                   c.age         AS coach_age,
                   c.preferred_formation
            FROM teams t
            JOIN coaches c ON c.team_id = t.id
            """;

    private static final String SELECT_ALL = SELECT_TEAMS_WITH_COACH + " ORDER BY t.name";
    private static final String SELECT_BY_ID = SELECT_TEAMS_WITH_COACH + " WHERE t.id = ?";

    private final ConnectionProvider connectionProvider;

    public TeamDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<Team> findById(Integer id) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapTeam(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load team " + id, e);
        }
    }

    @Override
    public List<Team> findAll() {
        List<Team> teams = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                teams.add(mapTeam(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load teams", e);
        }
        return teams;
    }

    private Team mapTeam(ResultSet resultSet) throws SQLException {
        Coach coach = new Coach(
                resultSet.getInt("coach_id"),
                resultSet.getString("coach_first_name"),
                resultSet.getString("coach_last_name"),
                resultSet.getString("coach_nationality"),
                resultSet.getInt("coach_age"),
                Formation.valueOf(resultSet.getString("preferred_formation")));

        return new Team(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("short_name"),
                resultSet.getString("city"),
                resultSet.getString("stadium"),
                coach);
    }
}