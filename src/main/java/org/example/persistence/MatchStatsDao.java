package org.example.persistence;

import org.example.engine.PlayerMatchStats;
import org.example.engine.TeamMatchStats;
import org.example.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class MatchStatsDao {

    private static final String INSERT_PLAYER_STATS = """
            INSERT INTO player_match_stats (match_id, player_id, team_id, is_starter, minutes_played, goals, assists,
                own_goals, shots, shots_on_target, key_passes, tackles, interceptions, fouls_committed, fouls_suffered,
                offsides, yellow_cards, red_cards, saves, goals_conceded, clean_sheet, rating)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_TEAM_STATS = """
            INSERT INTO team_match_stats (match_id, team_id, goals, shots, shots_on_target, corners, fouls,
                offsides, saves, yellow_cards, red_cards, possession_percent)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public void insertPlayerStats(Connection connection, int matchId, List<PlayerMatchStats> players) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_PLAYER_STATS)) {
            for (PlayerMatchStats stats : players) {
                int i = 1;
                statement.setInt(i++, matchId);
                statement.setInt(i++, stats.getPlayer().getId());
                statement.setInt(i++, stats.getTeam().getId());
                statement.setBoolean(i++, stats.isStarter());
                statement.setInt(i++, stats.getMinutesPlayed());
                statement.setInt(i++, stats.getGoals());
                statement.setInt(i++, stats.getAssists());
                statement.setInt(i++, stats.getOwnGoals());
                statement.setInt(i++, stats.getShots());
                statement.setInt(i++, stats.getShotsOnTarget());
                statement.setInt(i++, stats.getKeyPasses());
                statement.setInt(i++, stats.getTackles());
                statement.setInt(i++, stats.getInterceptions());
                statement.setInt(i++, stats.getFoulsCommitted());
                statement.setInt(i++, stats.getFoulsSuffered());
                statement.setInt(i++, stats.getOffsides());
                statement.setInt(i++, stats.getYellowCards());
                statement.setInt(i++, stats.getRedCards());
                statement.setInt(i++, stats.getSaves());
                statement.setInt(i++, stats.getGoalsConceded());
                statement.setBoolean(i++, stats.hasCleanSheet());
                if (stats.isRated()) {
                    statement.setDouble(i, stats.getRating());
                } else {
                    statement.setNull(i, Types.NUMERIC);
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public void insertTeamStats(Connection connection, int matchId, Team team, TeamMatchStats stats) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_TEAM_STATS)) {
            statement.setInt(1, matchId);
            statement.setInt(2, team.getId());
            statement.setInt(3, stats.goals());
            statement.setInt(4, stats.shots());
            statement.setInt(5, stats.shotsOnTarget());
            statement.setInt(6, stats.corners());
            statement.setInt(7, stats.fouls());
            statement.setInt(8, stats.offsides());
            statement.setInt(9, stats.saves());
            statement.setInt(10, stats.yellowCards());
            statement.setInt(11, stats.redCards());
            statement.setInt(12, stats.possessionPercent());
            statement.executeUpdate();
        }
    }
}
