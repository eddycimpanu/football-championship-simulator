package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.model.Coach;
import org.example.model.Team;

public class CoachCardEvent extends KeyMatchEvent {

    private final Coach coach;
    private final CardEvent.CardType cardType;

    public CoachCardEvent(MatchMinute minute, Team team, Coach coach, CardEvent.CardType cardType) {
        super(minute, team);
        if (coach == null || cardType == null) {
            throw new IllegalArgumentException("Coach and card type are required");
        }
        this.coach = coach;
        this.cardType = cardType;
    }

    @Override
    public EventType getType() {
        return cardType.isSendingOff() ? EventType.COACH_RED_CARD : EventType.COACH_YELLOW_CARD;
    }

    @Override
    public String describe() {
        String label = cardType.isSendingOff() ? "COACH SENT OFF" : "YELLOW CARD (coach)";
        return label + ": " + coach.getFullName() + " (" + getTeam().getName() + ") for protesting";
    }

    public Coach getCoach() { return coach; }
    public CardEvent.CardType getCardType() { return cardType; }
}
