package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.engine.Score;
import org.example.model.Player;
import org.example.model.Team;

public class GoalEvent extends KeyMatchEvent {

    public enum GoalType { NORMAL, HEADER, PENALTY, FREE_KICK, OWN_GOAL }

    private final Player scorer;
    private final Player assist;
    private final GoalType goalType;
    private final Score scoreAfter;

    public GoalEvent(MatchMinute minute, Team scoringTeam, Player scorer, Player assist,
                     GoalType goalType, Score scoreAfter) {
        super(minute, scoringTeam);
        this.scorer = required(scorer, "Scorer");
        this.assist = assist;
        this.goalType = goalType == null ? GoalType.NORMAL : goalType;
        this.scoreAfter = scoreAfter;
    }

    @Override
    public EventType getType() {
        return goalType == GoalType.OWN_GOAL ? EventType.OWN_GOAL : EventType.GOAL;
    }

    @Override
    public String describe() {
        if (goalType == GoalType.OWN_GOAL) {
            return "OWN GOAL by " + scorer.getFullName() + ", goal for " + getTeam().getName() + " [" + scoreAfter + "]";
        }
        String how = switch (goalType) {
            case HEADER -> " with a header";
            case PENALTY -> " from the penalty spot";
            case FREE_KICK -> " from a free kick";
            default -> "";
        };
        String assistText = assist == null ? "" : ", assist: " + assist.getFullName();
        return "GOAL! " + scorer.getFullName() + " (" + getTeam().getName() + ")" + how + assistText
                + " [" + scoreAfter + "]";
    }

    public Player getScorer() { return scorer; }
    public Player getAssist() { return assist; }
    public GoalType getGoalType() { return goalType; }
    public Score getScoreAfter() { return scoreAfter; }
}