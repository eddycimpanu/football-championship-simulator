package org.example.engine.event;

import org.example.engine.MatchMinute;
import org.example.engine.Score;

public class PeriodEvent extends KeyMatchEvent {

    public enum Kind {
        KICK_OFF("KICK-OFF", EventType.KICK_OFF),
        HALF_TIME("HALF-TIME", EventType.HALF_TIME),
        SECOND_HALF("SECOND HALF", EventType.SECOND_HALF),
        FULL_TIME("FULL-TIME", EventType.FULL_TIME);

        private final String label;
        private final EventType eventType;

        Kind(String label, EventType eventType) {
            this.label = label;
            this.eventType = eventType;
        }
    }

    private final Kind kind;
    private final Score score;

    public PeriodEvent(MatchMinute minute, Kind kind, Score score) {
        super(minute, null);
        this.kind = kind;
        this.score = score == null ? Score.ZERO : score;
    }

    @Override
    public EventType getType() {
        return kind.eventType;
    }

    @Override
    public String describe() {
        return kind == Kind.KICK_OFF ? kind.label : kind.label + " [" + score + "]";
    }

    public Kind getKind() { return kind; }
    public Score getScore() { return score; }
}