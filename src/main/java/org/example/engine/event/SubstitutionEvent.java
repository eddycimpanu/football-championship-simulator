package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Player;
import org.example.model.Team;

public class SubstitutionEvent extends KeyMatchEvent {

    public enum Reason { INJURY, FATIGUE, CARD_RISK, TACTICAL, GOALKEEPER_SENT_OFF }

    private final Player playerOut;
    private final Player playerIn;
    private final Reason reason;

    public SubstitutionEvent(MatchMinute minute, Team team, Player playerOut, Player playerIn, Reason reason) {
        super(minute, team);
        this.playerOut = required(playerOut, "Player going off");
        this.playerIn = required(playerIn, "Player coming on");
        if (playerOut == playerIn) {
            throw new IllegalArgumentException("A player cannot replace himself");
        }
        this.reason = reason == null ? Reason.TACTICAL : reason;
    }

    @Override
    public EventType getType() {
        return EventType.SUBSTITUTION;
    }

    @Override
    public String describe() {
        return "SUBSTITUTION (" + getTeam().getName() + "): " + playerIn.getFullName() + " on, "
                + playerOut.getFullName() + " off (" + reason.name().toLowerCase().replace('_', ' ') + ")";
    }

    public Player getPlayerOut() { return playerOut; }
    public Player getPlayerIn() { return playerIn; }
    public Reason getReason() { return reason; }
}
