package org.example.persistence;

import org.example.betting.MatchOdds;
import org.example.betting.OddsStore;
import org.example.betting.Selection;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// match_odds: one row per match and selection (7 rows per match).
public class OddsDao implements OddsStore {

    private static final String SELECT_BY_MATCHES = """
            SELECT match_id, selection, probability, odds, simulations
            FROM match_odds
            WHERE match_id = ANY (?)
            """;

    private static final String INSERT = """
            INSERT INTO match_odds (match_id, selection, probability, odds, simulations)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (match_id, selection) DO NOTHING
            """;

    private final ConnectionProvider connectionProvider;

    public OddsDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Map<Integer, MatchOdds> findByMatchIds(Collection<Integer> matchIds) {
        Map<Integer, Map<Selection, Double>> chances = new HashMap<>();
        Map<Integer, Map<Selection, Double>> odds = new HashMap<>();
        Map<Integer, Integer> simulations = new HashMap<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_MATCHES)) {
            Array ids = connection.createArrayOf("integer", matchIds.toArray());
            statement.setArray(1, ids);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int matchId = rs.getInt("match_id");
                    Selection selection = Selection.valueOf(rs.getString("selection"));
                    chances.computeIfAbsent(matchId, id -> new EnumMap<>(Selection.class))
                            .put(selection, rs.getDouble("probability"));
                    odds.computeIfAbsent(matchId, id -> new EnumMap<>(Selection.class))
                            .put(selection, rs.getDouble("odds"));
                    simulations.put(matchId, rs.getInt("simulations"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the odds", e);
        }

        Map<Integer, MatchOdds> result = new HashMap<>();
        chances.forEach((matchId, matchChances) -> {
            if (matchChances.size() == Selection.values().length) {   // only complete sets of odds
                result.put(matchId, new MatchOdds(matchId, simulations.get(matchId), matchChances, odds.get(matchId)));
            }
        });
        return result;
    }

    // All the odds of a round in one transaction
    @Override
    public void saveAll(List<MatchOdds> allOdds) {
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                for (MatchOdds matchOdds : allOdds) {
                    for (Selection selection : Selection.values()) {
                        statement.setInt(1, matchOdds.matchId());
                        statement.setString(2, selection.name());
                        statement.setDouble(3, matchOdds.probabilityOf(selection));
                        statement.setDouble(4, matchOdds.oddsFor(selection));
                        statement.setInt(5, matchOdds.simulations());
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the odds", e);
        }
    }
}
