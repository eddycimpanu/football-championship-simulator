package org.example.persistence;

import org.example.engine.event.CardEvent;
import org.example.engine.event.FoulEvent;
import org.example.engine.event.GoalEvent;
import org.example.engine.event.InjuryEvent;
import org.example.engine.event.MatchEvent;
import org.example.engine.event.OffsideEvent;
import org.example.engine.event.SaveEvent;
import org.example.engine.event.ShotEvent;
import org.example.engine.event.SubstitutionEvent;
import org.example.model.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class MatchEventDao {

    private static final String INSERT_EVENT = """
            INSERT INTO match_events (match_id, minute, added_time, event_type, team_id,
                                      player_id, secondary_player_id, is_key_event, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    // The main and the second player of each kind of event (pattern matching for instanceof)
    private record People(Player main, Player secondary) {
    }

    public void insertAll(Connection connection, int matchId, List<MatchEvent> events) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_EVENT)) {
            for (MatchEvent event : events) {
                People people = peopleOf(event);
                statement.setInt(1, matchId);
                statement.setInt(2, event.getMinute().minute());
                statement.setInt(3, event.getMinute().addedTime());
                statement.setString(4, event.getType().name());
                setOptionalId(statement, 5, event.getTeam() == null ? null : event.getTeam().getId());
                setOptionalId(statement, 6, people.main() == null ? null : people.main().getId());
                setOptionalId(statement, 7, people.secondary() == null ? null : people.secondary().getId());
                statement.setBoolean(8, event.isKeyEvent());
                statement.setString(9, event.describe());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private People peopleOf(MatchEvent event) {
        if (event instanceof GoalEvent goal) return new People(goal.getScorer(), goal.getAssist());
        if (event instanceof CardEvent card) return new People(card.getPlayer(), null);
        if (event instanceof SubstitutionEvent sub) return new People(sub.getPlayerIn(), sub.getPlayerOut());
        if (event instanceof InjuryEvent injury) return new People(injury.getPlayer(), null);
        if (event instanceof ShotEvent shot) return new People(shot.getShooter(), null);
        if (event instanceof SaveEvent save) return new People(save.getGoalkeeper(), save.getShooter());
        if (event instanceof FoulEvent foul) return new People(foul.getFouler(), foul.getFouled());
        if (event instanceof OffsideEvent offside) return new People(offside.getPlayer(), null);
        return new People(null, null);   // kick-off, half-time, corner, coach card...
    }

    private void setOptionalId(PreparedStatement statement, int index, Integer id) throws SQLException {
        if (id == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, id);
        }
    }
}
