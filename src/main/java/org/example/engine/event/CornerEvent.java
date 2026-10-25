package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Team;

public class CornerEvent extends MatchEvent {

    public CornerEvent(MatchMinute minute, Team team) {
        super(minute, team);
    }

    @Override
    public EventType getType() {
        return EventType.CORNER;
    }

    @Override
    public String describe() {
        return "Corner for " + getTeam().getName();
    }
}