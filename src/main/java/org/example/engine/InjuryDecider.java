package org.example.engine;

import org.example.config.EngineConfig;
import org.example.engine.event.InjuryEvent;
import org.example.model.Player;

// Injuries are rare: a small chance after every phase, a bit higher for a player who was just fouled.
public final class InjuryDecider {

    private InjuryDecider() {
        // utility class, no instances
    }

    public static void afterPhase(MatchContext context) {
        for (MatchTeam team : new MatchTeam[]{context.getHome(), context.getAway()}) {
            if (!team.getOnPitch().isEmpty() && context.random().chance(EngineConfig.INJURY_CHANCE_PER_PHASE)) {
                injure(context, team, context.random().pickOne(team.getOnPitch()));
            }
        }
    }

    public static void afterFoul(MatchContext context, Player fouled) {
        MatchTeam team = context.teamOf(fouled);
        if (team.isOnPitch(fouled) && context.random().chance(EngineConfig.INJURY_CHANCE_AFTER_FOUL)) {
            injure(context, team, fouled);
        }
    }

    private static void injure(MatchContext context, MatchTeam team, Player player) {
        team.injure(player);
        context.clock().recordInjury();
        context.addEvent(new InjuryEvent(context.minute(), team.getTeam(), player));
        SubstitutionManager.afterInjury(context, team, player);
    }
}
