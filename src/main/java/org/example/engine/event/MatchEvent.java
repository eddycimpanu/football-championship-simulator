package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Player;
import org.example.model.Team;

public abstract class MatchEvent {

    private final MatchMinute minute;
    private final Team team;

    protected MatchEvent(MatchMinute minute, Team team) {
        if (minute == null) {
            throw new IllegalArgumentException("Every event needs a minute");
        }
        this.minute = minute;
        this.team = team;
    }

    public abstract EventType getType();

    public abstract String describe();

    public boolean isKeyEvent() {
        return false;
    }

    public MatchMinute getMinute() {
        return minute;
    }

    public Team getTeam() {
        return team;
    }

    protected static Player required(Player player, String role) {
        if (player == null) {
            throw new IllegalArgumentException(role + " is required");
        }
        return player;
    }

    @Override
    public final String toString() {
        return String.format("%-7s %s", minute, describe());
    }
}