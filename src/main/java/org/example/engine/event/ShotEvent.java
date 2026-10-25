package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Player;
import org.example.model.Team;

public class ShotEvent extends MatchEvent {

    private final Player shooter;
    private final boolean onTarget;

    public ShotEvent(MatchMinute minute, Team team, Player shooter, boolean onTarget) {
        super(minute, team);
        this.shooter = required(shooter, "Shooter");
        this.onTarget = onTarget;
    }

    @Override
    public EventType getType() {
        return EventType.SHOT;
    }

    @Override
    public String describe() {
        return "Shot by " + shooter.getFullName() + " (" + getTeam().getName() + "), "
                + (onTarget ? "on target" : "off target");
    }

    public Player getShooter() { return shooter; }
    public boolean isOnTarget() { return onTarget; }
}