package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;
import org.example.engine.event.FoulEvent;
import org.example.model.Player;

public class FoulState extends PhaseState {

    private final Player fouler;
    private final Player fouled;
    private final boolean duringCross;

    public FoulState(Player fouler, Player fouled, boolean duringCross) {
        this.fouler = fouler;
        this.fouled = fouled;
        this.duringCross = duringCross;
    }

    @Override
    public StateResult execute(MatchContext context) {
        context.addEvent(new FoulEvent(context.minute(), context.defendingTeam().getTeam(), fouler, fouled));
        int seconds = duration(context, EngineConfig.FOUL_MIN, EngineConfig.FOUL_MAX);

        // Card check (no card, yellow or red) is added in stage 18.

        // Inside or outside the box? Fouls during a cross happen in the box much more often.
        double insideChance = duringCross ? EngineConfig.PENALTY_CHANCE_FROM_CROSS : EngineConfig.PENALTY_CHANCE_OPEN_PLAY;
        insideChance *= fouler.getAttributes().aggression() / EngineConfig.REFERENCE_ATTRIBUTE;

        // The fouled team keeps the ball
        if (context.random().chance(Math.min(1, insideChance))) {
            return new StateResult(new PenaltyState(), seconds);
        }
        return new StateResult(new FreeKickState(true), seconds);
    }
}
