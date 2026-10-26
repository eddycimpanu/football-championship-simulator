package org.example.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Season statistics calculated in SQL from the saved matches.
public class PlayerStatsDao {

    public record SeasonTotals(int appearances, int starts, int minutes, int goals, int assists, int shots,
                               int shotsOnTarget, int keyPasses, int tackles, int interceptions,
                               int yellowCards, int redCards, int saves, int cleanSheets, Double averageRating) {
    }

    public record MatchLine(int round, String opponent, boolean home, String score, int minutes,
                            int goals, int assists, Double rating) {
    }

    public record ScorerLine(String player, String team, int goals, int assists, int minutes) {
    }

    private static final String SEASON_TOTALS = """
            SELECT COUNT(*) AS appearances,
                   COUNT(*) FILTER (WHERE s.is_starter) AS starts,
                   COALESCE(SUM(s.minutes_played), 0) AS minutes,
                   COALESCE(SUM(s.goals), 0) AS goals,
                   COALESCE(SUM(s.assists), 0) AS assists,
                   COALESCE(SUM(s.shots), 0) AS shots,
                   COALESCE(SUM(s.shots_on_target), 0) AS shots_on_target,
                   COALESCE(SUM(s.key_passes), 0) AS key_passes,
                   COALESCE(SUM(s.tackles), 0) AS tackles,
                   COALESCE(SUM(s.interceptions), 0) AS interceptions,
                   COALESCE(SUM(s.yellow_cards), 0) AS yellow_cards,
                   COALESCE(SUM(s.red_cards), 0) AS red_cards,
                   COALESCE(SUM(s.saves), 0) AS saves,
                   COUNT(*) FILTER (WHERE s.clean_sheet) AS clean_sheets,
                   ROUND(AVG(s.rating), 2) AS average_rating
            FROM player_match_stats s
            JOIN matches m ON m.id = s.match_id
            WHERE m.season_id = ? AND s.player_id = ?
            """;

    private static final String MATCH_LINES = """
            SELECT m.round,
                   CASE WHEN m.home_team_id = s.team_id THEN away.name ELSE home.name END AS opponent,
                   m.home_team_id = s.team_id AS at_home,
                   m.home_goals || '-' || m.away_goals AS score,
                   s.minutes_played, s.goals, s.assists, s.rating
            FROM player_match_stats s
            JOIN matches m ON m.id = s.match_id
            JOIN teams home ON home.id = m.home_team_id
            JOIN teams away ON away.id = m.away_team_id
            WHERE m.season_id = ? AND s.player_id = ?
            ORDER BY m.round
            """;

    private static final String TOP_SCORERS = """
            SELECT p.first_name, p.last_name, t.name AS team,
                   SUM(s.goals) AS goals, SUM(s.assists) AS assists, SUM(s.minutes_played) AS minutes
            FROM player_match_stats s
            JOIN matches m ON m.id = s.match_id
            JOIN players p ON p.id = s.player_id
            JOIN teams t ON t.id = s.team_id
            WHERE m.season_id = ?
            GROUP BY p.id, p.first_name, p.last_name, t.name
            HAVING SUM(s.goals) > 0
            ORDER BY goals DESC, minutes ASC
            LIMIT ?
            """;

    private final ConnectionProvider connectionProvider;

    public PlayerStatsDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public SeasonTotals findSeasonTotals(int seasonId, int playerId) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEASON_TOTALS)) {
            statement.setInt(1, seasonId);
            statement.setInt(2, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                double rating = rs.getDouble("average_rating");
                return new SeasonTotals(rs.getInt("appearances"), rs.getInt("starts"), rs.getInt("minutes"),
                        rs.getInt("goals"), rs.getInt("assists"), rs.getInt("shots"), rs.getInt("shots_on_target"),
                        rs.getInt("key_passes"), rs.getInt("tackles"), rs.getInt("interceptions"),
                        rs.getInt("yellow_cards"), rs.getInt("red_cards"), rs.getInt("saves"),
                        rs.getInt("clean_sheets"), rs.wasNull() ? null : rating);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the season statistics of player " + playerId, e);
        }
    }

    public List<MatchLine> findMatchLines(int seasonId, int playerId) {
        List<MatchLine> lines = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(MATCH_LINES)) {
            statement.setInt(1, seasonId);
            statement.setInt(2, playerId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    double rating = rs.getDouble("rating");
                    Double value = rs.wasNull() ? null : rating;
                    lines.add(new MatchLine(rs.getInt("round"), rs.getString("opponent"), rs.getBoolean("at_home"),
                            rs.getString("score"), rs.getInt("minutes_played"), rs.getInt("goals"),
                            rs.getInt("assists"), value));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the matches of player " + playerId, e);
        }
        return lines;
    }

    public List<ScorerLine> findTopScorers(int seasonId, int limit) {
        List<ScorerLine> lines = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(TOP_SCORERS)) {
            statement.setInt(1, seasonId);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String first = rs.getString("first_name");
                    String name = first == null || first.isBlank() ? rs.getString("last_name") : first + " " + rs.getString("last_name");
                    lines.add(new ScorerLine(name, rs.getString("team"), rs.getInt("goals"), rs.getInt("assists"), rs.getInt("minutes")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the top scorers of season " + seasonId, e);
        }
        return lines;
    }
}
