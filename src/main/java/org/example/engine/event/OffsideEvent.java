package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Player;
import org.example.model.Team;

public class OffsideEvent extends MatchEvent {

    private final Player player;

    public OffsideEvent(MatchMinute minute, Team team, Player player) {
        super(minute, team);
        this.player = required(player, "Player");
    }

    @Override
    public EventType getType() {
        return EventType.OFFSIDE;
    }

    @Override
    public String describe() {
        return "Offside: " + player.getFullName() + " (" + getTeam().getName() + ")";
    }

    public Player getPlayer() { return player; }
}