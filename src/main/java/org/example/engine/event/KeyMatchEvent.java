package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Team;

public abstract class KeyMatchEvent extends MatchEvent {

    protected KeyMatchEvent(MatchMinute minute, Team team) {
        super(minute, team);
    }

    @Override
    public final boolean isKeyEvent() {
        return true;
    }
}