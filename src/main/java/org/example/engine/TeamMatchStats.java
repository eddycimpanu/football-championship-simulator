package org.example.engine;

import org.example.engine.event.CardEvent;
import org.example.engine.event.CornerEvent;
import org.example.engine.event.FoulEvent;
import org.example.engine.event.MatchEvent;
import org.example.engine.event.ShotEvent;

// Team statistics of one match. Stage 20 adds more (offsides, saves).
public record TeamMatchStats(int goals, int shots, int shotsOnTarget, int corners, int fouls,
                             int yellowCards, int redCards, int possessionPercent) {

    public static TeamMatchStats from(MatchContext context, MatchTeam team) {
        int goals = team == context.getHome() ? context.score().home() : context.score().away();

        int shots = 0;
        int onTarget = 0;
        for (ShotEvent shot : context.events().getEventsOfType(ShotEvent.class)) {
            if (shot.getTeam() == team.getTeam()) {
                shots++;
                if (shot.isOnTarget()) {
                    onTarget++;
                }
            }
        }
        // A second yellow counts as a yellow card and as a red card
        int yellows = 0;
        int reds = 0;
        for (CardEvent card : context.events().getEventsOfType(CardEvent.class)) {
            if (card.getTeam() == team.getTeam()) {
                if (card.getCardType() != CardEvent.CardType.RED) {
                    yellows++;
                }
                if (card.isSendingOff()) {
                    reds++;
                }
            }
        }
        return new TeamMatchStats(goals, shots, onTarget,
                count(context, CornerEvent.class, team),
                count(context, FoulEvent.class, team),
                yellows, reds,
                context.possessionPercent(team));
    }

    private static int count(MatchContext context, Class<? extends MatchEvent> type, MatchTeam team) {
        return (int) context.events().getEventsOfType(type).stream()
                .filter(event -> event.getTeam() == team.getTeam())
                .count();
    }
}
