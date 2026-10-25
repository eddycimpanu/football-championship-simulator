package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Player;
import org.example.model.Team;

public class SaveEvent extends MatchEvent {

    private final Player goalkeeper;
    private final Player shooter;

    public SaveEvent(MatchMinute minute, Team goalkeeperTeam, Player goalkeeper, Player shooter) {
        super(minute, goalkeeperTeam);
        this.goalkeeper = required(goalkeeper, "Goalkeeper");
        this.shooter = required(shooter, "Shooter");
    }

    @Override
    public EventType getType() {
        return EventType.SAVE;
    }

    @Override
    public String describe() {
        return "Save by " + goalkeeper.getFullName() + " (" + getTeam().getName() + ") from " + shooter.getFullName();
    }

    public Player getGoalkeeper() { return goalkeeper; }
    public Player getShooter() { return shooter; }
}