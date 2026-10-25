package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Player;
import org.example.model.Team;

public class InjuryEvent extends KeyMatchEvent {

    private final Player player;

    public InjuryEvent(MatchMinute minute, Team team, Player player) {
        super(minute, team);
        this.player = required(player, "Player");
    }

    @Override
    public EventType getType() {
        return EventType.INJURY;
    }

    @Override
    public String describe() {
        return "INJURY: " + player.getFullName() + " (" + getTeam().getName() + ") cannot continue";
    }

    public Player getPlayer() { return player; }
}
