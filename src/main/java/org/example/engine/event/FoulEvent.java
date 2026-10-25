package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Player;
import org.example.model.Team;

public class FoulEvent extends MatchEvent {

    private final Player fouler;
    private final Player fouled;

    public FoulEvent(MatchMinute minute, Team foulerTeam, Player fouler, Player fouled) {
        super(minute, foulerTeam);
        this.fouler = required(fouler, "Fouler");
        this.fouled = required(fouled, "Fouled player");
    }

    @Override
    public EventType getType() {
        return EventType.FOUL;
    }

    @Override
    public String describe() {
        return "Foul by " + fouler.getFullName() + " (" + getTeam().getName() + ") on " + fouled.getFullName();
    }

    public Player getFouler() { return fouler; }
    public Player getFouled() { return fouled; }
}