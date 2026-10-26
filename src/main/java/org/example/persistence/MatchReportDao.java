package org.example.persistence;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Reads a saved match back from the database: score, events, team statistics and player ratings.
// It works for every match of the season, also the ones played before the program was restarted.
public class MatchReportDao {

    public record MatchInfo(int id, int round, String homeTeam, String awayTeam, boolean played,
                            int homeGoals, int awayGoals, String referee) {
    }

    public record EventLine(int minute, int addedTime, String description) {
    }

    public record TeamStatsLine(String team, int goals, int shots, int shotsOnTarget, int corners, int fouls,
                                int offsides, int saves, int yellowCards, int redCards, int possessionPercent) {
    }

    public record PlayerLine(String team, String player, String position, boolean starter, int minutes,
                             int goals, int assists, int shots, int shotsOnTarget, int keyPasses, int tackles,
                             int interceptions, int fouls, int yellowCards, int redCards, int saves, Double rating) {
    }

    private static final String FULL_NAME = "TRIM(%1$s.first_name || ' ' || %1$s.last_name)";

    // A pair of teams meets only once at each ground, so home + away finds exactly one match
    private static final String FIND_MATCH = """
            SELECT m.id, m.round, h.name AS home_team, a.name AS away_team, m.status, m.home_goals, m.away_goals,
                   %s AS referee
            FROM matches m
            JOIN teams h ON h.id = m.home_team_id
            JOIN teams a ON a.id = m.away_team_id
            LEFT JOIN referees r ON r.id = m.referee_id
            WHERE m.season_id = ? AND m.home_team_id = ? AND m.away_team_id = ?
            """.formatted(FULL_NAME.formatted("r"));

    private static final String EVENTS = """
            SELECT minute, added_time, description
            FROM match_events
            WHERE match_id = ? AND (is_key_event OR NOT ?)
            ORDER BY id
            """;

    // Home team first
    private static final String TEAM_STATS = """
            SELECT t.short_name, s.goals, s.shots, s.shots_on_target, s.corners, s.fouls, s.offsides, s.saves,
                   s.yellow_cards, s.red_cards, s.possession_percent
            FROM team_match_stats s
            JOIN teams t ON t.id = s.team_id
            JOIN matches m ON m.id = s.match_id
            WHERE s.match_id = ?
            ORDER BY (s.team_id = m.home_team_id) DESC
            """;

    // Home team first, starters before substitutes, then by position like a team sheet
    private static final String PLAYER_LINES = """
            SELECT t.name AS team, %s AS player, p.position, s.is_starter, s.minutes_played, s.goals, s.assists,
                   s.shots, s.shots_on_target, s.key_passes, s.tackles, s.interceptions, s.fouls_committed,
                   s.yellow_cards, s.red_cards, s.saves, s.rating
            FROM player_match_stats s
            JOIN players p ON p.id = s.player_id
            JOIN teams t ON t.id = s.team_id
            JOIN matches m ON m.id = s.match_id
            WHERE s.match_id = ?
            ORDER BY (s.team_id = m.home_team_id) DESC, s.is_starter DESC,
                     CASE WHEN p.position = 'GK' THEN 0
                          WHEN p.position IN ('CB', 'LB', 'RB', 'LWB', 'RWB') THEN 1
                          WHEN p.position IN ('CDM', 'CM', 'CAM', 'LM', 'RM') THEN 2
                          ELSE 3 END,
                     p.shirt_number
            """.formatted(FULL_NAME.formatted("p"));

    private final ConnectionProvider connectionProvider;

    public MatchReportDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Optional<MatchInfo> findMatch(int seasonId, int homeTeamId, int awayTeamId) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_MATCH)) {
            statement.setInt(1, seasonId);
            statement.setInt(2, homeTeamId);
            statement.setInt(3, awayTeamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new MatchInfo(resultSet.getInt("id"), resultSet.getInt("round"),
                        resultSet.getString("home_team"), resultSet.getString("away_team"),
                        "PLAYED".equals(resultSet.getString("status")),
                        resultSet.getInt("home_goals"), resultSet.getInt("away_goals"),
                        resultSet.getString("referee")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not find the match", e);
        }
    }

    public List<EventLine> findEvents(int matchId, boolean keyEventsOnly) {
        List<EventLine> events = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(EVENTS)) {
            statement.setInt(1, matchId);
            statement.setBoolean(2, keyEventsOnly);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(new EventLine(resultSet.getInt("minute"), resultSet.getInt("added_time"),
                            resultSet.getString("description")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the events of match " + matchId, e);
        }
        return events;
    }

    public List<TeamStatsLine> findTeamStats(int matchId) {
        List<TeamStatsLine> lines = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(TEAM_STATS)) {
            statement.setInt(1, matchId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lines.add(new TeamStatsLine(resultSet.getString("short_name"), resultSet.getInt("goals"),
                            resultSet.getInt("shots"), resultSet.getInt("shots_on_target"), resultSet.getInt("corners"),
                            resultSet.getInt("fouls"), resultSet.getInt("offsides"), resultSet.getInt("saves"),
                            resultSet.getInt("yellow_cards"), resultSet.getInt("red_cards"),
                            resultSet.getInt("possession_percent")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the statistics of match " + matchId, e);
        }
        return lines;
    }

    public List<PlayerLine> findPlayerLines(int matchId) {
        List<PlayerLine> lines = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(PLAYER_LINES)) {
            statement.setInt(1, matchId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    BigDecimal rating = resultSet.getBigDecimal("rating");
                    lines.add(new PlayerLine(resultSet.getString("team"), resultSet.getString("player"),
                            resultSet.getString("position"), resultSet.getBoolean("is_starter"),
                            resultSet.getInt("minutes_played"), resultSet.getInt("goals"), resultSet.getInt("assists"),
                            resultSet.getInt("shots"), resultSet.getInt("shots_on_target"),
                            resultSet.getInt("key_passes"), resultSet.getInt("tackles"),
                            resultSet.getInt("interceptions"), resultSet.getInt("fouls_committed"),
                            resultSet.getInt("yellow_cards"), resultSet.getInt("red_cards"), resultSet.getInt("saves"),
                            rating == null ? null : rating.doubleValue()));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the player ratings of match " + matchId, e);
        }
        return lines;
    }
}
