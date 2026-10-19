package org.example.persistence;

import org.example.model.Referee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RefereeDao implements Repository<Referee, Integer> {

    private static final String SELECT_ALL =
            "SELECT id, first_name, last_name, nationality, age, strictness FROM referees ORDER BY last_name";
    private static final String SELECT_BY_ID =
            "SELECT id, first_name, last_name, nationality, age, strictness FROM referees WHERE id = ?";

    private final ConnectionProvider connectionProvider;

    public RefereeDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<Referee> findById(Integer id) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapReferee(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load referee " + id, e);
        }
    }

    @Override
    public List<Referee> findAll() {
        List<Referee> referees = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                referees.add(mapReferee(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load referees", e);
        }
        return referees;
    }

    private Referee mapReferee(ResultSet resultSet) throws SQLException {
        return new Referee(
                resultSet.getInt("id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("nationality"),
                resultSet.getInt("age"),
                resultSet.getInt("strictness"));
    }
}