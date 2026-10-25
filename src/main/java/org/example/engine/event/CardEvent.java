package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Player;
import org.example.model.Team;

public class CardEvent extends KeyMatchEvent {

    public enum CardType {
        YELLOW("YELLOW CARD", EventType.YELLOW_CARD),
        SECOND_YELLOW("SECOND YELLOW, SENT OFF", EventType.SECOND_YELLOW),
        RED("RED CARD", EventType.RED_CARD);

        private final String label;
        private final EventType eventType;

        CardType(String label, EventType eventType) {
            this.label = label;
            this.eventType = eventType;
        }
    }

    private final Player player;
    private final CardType cardType;

    public CardEvent(MatchMinute minute, Team team, Player player, CardType cardType) {
        super(minute, team);
        this.player = required(player, "Player");
        if (cardType == null) {
            throw new IllegalArgumentException("Card type is required");
        }
        this.cardType = cardType;
    }

    @Override
    public EventType getType() {
        return cardType.eventType;
    }

    @Override
    public String describe() {
        return cardType.label + ": " + player.getFullName() + " (" + getTeam().getName() + ")";
    }

    public boolean isSendingOff() {
        return cardType != CardType.YELLOW;
    }

    public Player getPlayer() { return player; }
    public CardType getCardType() { return cardType; }
}