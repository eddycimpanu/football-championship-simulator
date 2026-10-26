package org.example.persistence;

import org.example.model.Player;
import org.example.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlayerSeasonStateDao {

    // Insert the first time, update every round after that (PostgreSQL "upsert")
    private static final String UPSERT_STATE = """
            INSERT INTO player_season_state (season_id, player_id, condition, yellow_cards_accumulated,
                                             matches_suspended, matches_injured)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (season_id, player_id) DO UPDATE SET
                condition = EXCLUDED.condition,
                yellow_cards_accumulated = EXCLUDED.yellow_cards_accumulated,
                matches_suspended = EXCLUDED.matches_suspended,
                matches_injured = EXCLUDED.matches_injured
            """;

    private static final String SELECT_BY_SEASON = """
            SELECT player_id, condition, yellow_cards_accumulated, matches_suspended, matches_injured
            FROM player_season_state
            WHERE season_id = ?
            """;

    public void saveAll(Connection connection, int seasonId, List<Team> teams) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_STATE)) {
            for (Team team : teams) {
                for (Player player : team.getSquad()) {
                    statement.setInt(1, seasonId);
                    statement.setInt(2, player.getId());
                    statement.setInt(3, player.getCondition());
                    statement.setInt(4, player.getYellowCardsAccumulated());
                    statement.setInt(5, player.getMatchesSuspended());
                    statement.setInt(6, player.getMatchesInjured());
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    // Gives every player back the fitness, cards, bans and injuries he had when the season was saved.
    // Players without a saved row (no round finished yet) keep their fresh state.
    public void restoreAll(Connection connection, int seasonId, List<Team> teams) throws SQLException {
        Map<Integer, Player> playersById = teams.stream()
                .flatMap(team -> team.getSquad().stream())
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_SEASON)) {
            statement.setInt(1, seasonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Player player = playersById.get(resultSet.getInt("player_id"));
                    if (player != null) {
                        player.restoreState(resultSet.getInt("condition"), resultSet.getInt("yellow_cards_accumulated"),
                                resultSet.getInt("matches_suspended"), resultSet.getInt("matches_injured"));
                    }
                }
            }
        }
    }
}
