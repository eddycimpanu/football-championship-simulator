package org.example.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// End-of-season reports, all calculated in SQL from the saved matches:
// assists, discipline, goalkeepers, ratings, team of the season, team statistics and records.
public class SeasonReportDao {

    public record PlayerTotal(String player, String team, String position, String fieldRole, int appearances,
                              int minutes, int goals, int assists, int yellowCards, int redCards, int saves,
                              int goalsConceded, int cleanSheets, Double averageRating, int playerId,
                              String teamShort) {
    }

    public record TeamTotal(String team, int played, int goalsFor, int goalsAgainst, int cleanSheets,
                            double shotsPerMatch, int shotAccuracy, int possession, double cornersPerMatch,
                            int yellowCards, int redCards, int teamId, String teamShort) {
    }

    public record MatchRecord(int round, String homeTeam, String awayTeam, int homeGoals, int awayGoals,
                              int matchId, String homeShort, String awayShort) {
    }

    public record HatTrick(int round, String player, String team, String opponent, int goals,
                           int matchId, int playerId, String teamShort) {
    }

    // One row per player with his season totals. Every player query below starts from this CTE.
    private static final String PLAYER_TOTALS = """
            WITH totals AS (
                SELECT s.player_id, s.team_id,
                       COUNT(*)                                AS appearances,
                       SUM(s.minutes_played)                   AS minutes,
                       SUM(s.goals)                            AS goals,
                       SUM(s.assists)                          AS assists,
                       SUM(s.yellow_cards)                     AS yellow_cards,
                       SUM(s.red_cards)                        AS red_cards,
                       SUM(s.saves)                            AS saves,
                       SUM(s.goals_conceded)                   AS goals_conceded,
                       COUNT(*) FILTER (WHERE s.clean_sheet)   AS clean_sheets,
                       ROUND(AVG(s.rating), 2)                 AS average_rating
                FROM player_match_stats s
                JOIN matches m ON m.id = s.match_id
                WHERE m.season_id = ?
                GROUP BY s.player_id, s.team_id
            ),
            players_with_totals AS (
                SELECT TRIM(p.first_name || ' ' || p.last_name) AS player, t.name AS team, t.short_name AS team_short,
                       p.position,
                       CASE WHEN p.position = 'GK' THEN 'GK'
                            WHEN p.position IN ('CB', 'LB', 'RB', 'LWB', 'RWB') THEN 'DEF'
                            WHEN p.position IN ('CDM', 'CM', 'CAM', 'LM', 'RM') THEN 'MID'
                            ELSE 'FWD' END AS field_role,
                       x.*
                FROM totals x
                JOIN players p ON p.id = x.player_id
                JOIN teams t ON t.id = x.team_id
            )
            """;

    private static final String TOP_ASSISTS = PLAYER_TOTALS + """
            SELECT * FROM players_with_totals
            WHERE assists > 0
            ORDER BY assists DESC, goals DESC, minutes
            LIMIT ?
            """;

    // A red card counts as three yellows
    private static final String DISCIPLINE = PLAYER_TOTALS + """
            SELECT * FROM players_with_totals
            WHERE yellow_cards + red_cards > 0
            ORDER BY 3 * red_cards + yellow_cards DESC, red_cards DESC, minutes
            LIMIT ?
            """;

    private static final String GOALKEEPERS = PLAYER_TOTALS + """
            SELECT * FROM players_with_totals
            WHERE position = 'GK' AND minutes >= ?
            ORDER BY clean_sheets DESC, goals_conceded * 90.0 / minutes, saves DESC
            LIMIT ?
            """;

    private static final String BEST_RATINGS = PLAYER_TOTALS + """
            SELECT * FROM players_with_totals
            WHERE minutes >= ? AND average_rating IS NOT NULL
            ORDER BY average_rating DESC, goals + assists DESC
            LIMIT ?
            """;

    // The best average rating for each place of a 4-3-3 (window function):
    // 1 GK, a left-back, 2 centre-backs, a right-back, 3 midfielders and 3 forwards
    private static final String TEAM_OF_THE_SEASON = PLAYER_TOTALS + """
            , with_slot AS (
                SELECT *, CASE WHEN position IN ('LB', 'LWB') THEN 'LB'
                               WHEN position IN ('RB', 'RWB') THEN 'RB'
                               ELSE field_role END AS slot
                FROM players_with_totals
                WHERE minutes >= ? AND average_rating IS NOT NULL
            ),
            ranked AS (
                SELECT *, ROW_NUMBER() OVER (PARTITION BY slot
                                             ORDER BY average_rating DESC, goals + assists DESC) AS place
                FROM with_slot
            )
            SELECT * FROM ranked
            WHERE place <= CASE slot WHEN 'GK' THEN 1 WHEN 'LB' THEN 1 WHEN 'RB' THEN 1
                                     WHEN 'DEF' THEN 2 ELSE 3 END
            ORDER BY CASE slot WHEN 'GK' THEN 0 WHEN 'LB' THEN 1 WHEN 'DEF' THEN 2 WHEN 'RB' THEN 3
                               WHEN 'MID' THEN 4 ELSE 5 END, place
            """;

    private static final String TEAM_TOTALS = """
            SELECT t.name AS team, t.id AS team_id, t.short_name AS team_short,
                   COUNT(*)                                                       AS played,
                   SUM(s.goals)                                                   AS goals_for,
                   SUM(CASE WHEN s.team_id = m.home_team_id THEN m.away_goals ELSE m.home_goals END) AS goals_against,
                   COUNT(*) FILTER (WHERE CASE WHEN s.team_id = m.home_team_id
                                               THEN m.away_goals ELSE m.home_goals END = 0) AS clean_sheets,
                   ROUND(AVG(s.shots), 1)                                         AS shots_per_match,
                   ROUND(100.0 * SUM(s.shots_on_target) / NULLIF(SUM(s.shots), 0)) AS shot_accuracy,
                   ROUND(AVG(s.possession_percent))                               AS possession,
                   ROUND(AVG(s.corners), 1)                                       AS corners_per_match,
                   SUM(s.yellow_cards)                                            AS yellow_cards,
                   SUM(s.red_cards)                                               AS red_cards
            FROM team_match_stats s
            JOIN matches m ON m.id = s.match_id
            JOIN teams t ON t.id = s.team_id
            WHERE m.season_id = ?
            GROUP BY t.name, t.id, t.short_name
            ORDER BY goals_for DESC, goals_against
            """;

    private static final String MATCH_RECORD = """
            SELECT m.round, m.id AS match_id, h.name AS home_team, a.name AS away_team, m.home_goals, m.away_goals,
                   h.short_name AS home_short, a.short_name AS away_short
            FROM matches m
            JOIN teams h ON h.id = m.home_team_id
            JOIN teams a ON a.id = m.away_team_id
            WHERE m.season_id = ? AND m.status = 'PLAYED'
            ORDER BY %s, m.round
            LIMIT ?
            """;
    private static final String BIGGEST_WINS =
            MATCH_RECORD.formatted("ABS(m.home_goals - m.away_goals) DESC, m.home_goals + m.away_goals DESC");
    private static final String HIGHEST_SCORING =
            MATCH_RECORD.formatted("m.home_goals + m.away_goals DESC, ABS(m.home_goals - m.away_goals)");

    private static final String HAT_TRICKS = """
            SELECT m.round, m.id AS match_id, p.id AS player_id, t.short_name AS team_short,
                   TRIM(p.first_name || ' ' || p.last_name) AS player, t.name AS team,
                   CASE WHEN s.team_id = m.home_team_id THEN a.name ELSE h.name END AS opponent, s.goals
            FROM player_match_stats s
            JOIN matches m ON m.id = s.match_id
            JOIN players p ON p.id = s.player_id
            JOIN teams t ON t.id = s.team_id
            JOIN teams h ON h.id = m.home_team_id
            JOIN teams a ON a.id = m.away_team_id
            WHERE m.season_id = ? AND s.goals >= 3
            ORDER BY s.goals DESC, m.round
            """;

    private final ConnectionProvider connectionProvider;

    public SeasonReportDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public List<PlayerTotal> findTopAssists(int seasonId, int limit) {
        return query(TOP_ASSISTS, "the top assists", SeasonReportDao::playerTotal, seasonId, limit);
    }

    public List<PlayerTotal> findMostCarded(int seasonId, int limit) {
        return query(DISCIPLINE, "the discipline table", SeasonReportDao::playerTotal, seasonId, limit);
    }

    public List<PlayerTotal> findGoalkeepers(int seasonId, int minMinutes, int limit) {
        return query(GOALKEEPERS, "the goalkeepers", SeasonReportDao::playerTotal, seasonId, minMinutes, limit);
    }

    public List<PlayerTotal> findBestRatings(int seasonId, int minMinutes, int limit) {
        return query(BEST_RATINGS, "the best ratings", SeasonReportDao::playerTotal, seasonId, minMinutes, limit);
    }

    public List<PlayerTotal> findTeamOfTheSeason(int seasonId, int minMinutes) {
        return query(TEAM_OF_THE_SEASON, "the team of the season", SeasonReportDao::playerTotal, seasonId, minMinutes);
    }

    public List<TeamTotal> findTeamTotals(int seasonId) {
        return query(TEAM_TOTALS, "the team statistics", rs -> new TeamTotal(rs.getString("team"),
                rs.getInt("played"), rs.getInt("goals_for"), rs.getInt("goals_against"), rs.getInt("clean_sheets"),
                rs.getDouble("shots_per_match"), rs.getInt("shot_accuracy"), rs.getInt("possession"),
                rs.getDouble("corners_per_match"), rs.getInt("yellow_cards"), rs.getInt("red_cards"),
                rs.getInt("team_id"), rs.getString("team_short")), seasonId);
    }

    public List<MatchRecord> findBiggestWins(int seasonId, int limit) {
        return query(BIGGEST_WINS, "the biggest wins", SeasonReportDao::matchRecord, seasonId, limit);
    }

    public List<MatchRecord> findHighestScoring(int seasonId, int limit) {
        return query(HIGHEST_SCORING, "the highest-scoring matches", SeasonReportDao::matchRecord, seasonId, limit);
    }

    public List<HatTrick> findHatTricks(int seasonId) {
        return query(HAT_TRICKS, "the hat-tricks", rs -> new HatTrick(rs.getInt("round"), rs.getString("player"),
                rs.getString("team"), rs.getString("opponent"), rs.getInt("goals"), rs.getInt("match_id"),
                rs.getInt("player_id"), rs.getString("team_short")), seasonId);
    }

    // ----- Mapping rows -----

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    private static PlayerTotal playerTotal(ResultSet rs) throws SQLException {
        double rating = rs.getDouble("average_rating");
        Double averageRating = rs.wasNull() ? null : rating;
        return new PlayerTotal(rs.getString("player"), rs.getString("team"), rs.getString("position"),
                rs.getString("field_role"), rs.getInt("appearances"), rs.getInt("minutes"), rs.getInt("goals"),
                rs.getInt("assists"), rs.getInt("yellow_cards"), rs.getInt("red_cards"), rs.getInt("saves"),
                rs.getInt("goals_conceded"), rs.getInt("clean_sheets"), averageRating, rs.getInt("player_id"),
                rs.getString("team_short"));
    }

    private static MatchRecord matchRecord(ResultSet rs) throws SQLException {
        return new MatchRecord(rs.getInt("round"), rs.getString("home_team"), rs.getString("away_team"),
                rs.getInt("home_goals"), rs.getInt("away_goals"), rs.getInt("match_id"), rs.getString("home_short"),
                rs.getString("away_short"));
    }

    // Runs a SELECT with int parameters (in order) and turns every row into an object
    private <T> List<T> query(String sql, String what, RowMapper<T> mapper, int... parameters) {
        List<T> rows = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setInt(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load " + what, e);
        }
        return rows;
    }
}
