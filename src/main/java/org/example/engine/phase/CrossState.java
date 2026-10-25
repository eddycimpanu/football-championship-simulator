package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;
import org.example.engine.MatchTeam;
import org.example.engine.PlayerAction;
import org.example.engine.PlayerPicker;
import org.example.engine.event.GoalEvent;
import org.example.engine.random.RouletteWheel;
import org.example.engine.random.SkillFactor;
import org.example.model.Attribute;
import org.example.model.FieldRole;
import org.example.model.Player;

public class CrossState extends PhaseState {

    private enum Outcome { HEADER, DEFENDED, FOUL, OWN_GOAL }

    private final Player crosser;

    public CrossState(Player crosser) {
        this.crosser = crosser;
    }

    @Override
    public StateResult execute(MatchContext context) {
        MatchTeam attackers = context.attackingTeam();
        MatchTeam defenders = context.defendingTeam();
        int seconds = duration(context, EngineConfig.CROSS_MIN, EngineConfig.CROSS_MAX);

        Player target = PlayerPicker.pick(attackers, PlayerAction.HEADER, crosser, context.random());

        // Crosser's PASSING and target's HEADING vs defenders' HEADING, PHYSICAL and the keeper's HANDLING
        double attack = (context.attributes(crosser).passing() + context.attributes(target).heading()) / 2.0;
        double defence = (defenders.averageOf(Attribute.HEADING, FieldRole.DEFENDER)
                + defenders.averageOf(Attribute.PHYSICAL, FieldRole.DEFENDER)
                + context.attributes(defenders.getGoalkeeper()).handling()) / 3.0;
        double skill = SkillFactor.of(attack, defence) * context.matchupFactor();
        double aggression = defenders.averageOf(Attribute.AGGRESSION, FieldRole.DEFENDER) / EngineConfig.REFERENCE_ATTRIBUTE;

        Outcome outcome = new RouletteWheel<Outcome>()
                .addFavourable(Outcome.HEADER, EngineConfig.CROSS_HEADER, skill)
                .addUnfavourable(Outcome.DEFENDED, EngineConfig.CROSS_DEFENDED, skill)
                .addOption(Outcome.FOUL, EngineConfig.CROSS_FOUL * aggression)
                .addOption(Outcome.OWN_GOAL, EngineConfig.CROSS_OWN_GOAL)
                .spin(context.random());

        return switch (outcome) {
            case HEADER -> {
                context.recordKeyPass(crosser);
                yield new StateResult(new ShootState(ShotType.HEADER, target, crosser), seconds);
            }
            case DEFENDED -> {
                context.recordInterception(PlayerPicker.pick(defenders, PlayerAction.DEFEND, context.random()));
                context.switchPossession();
                yield new StateResult(new DispossessionState(), seconds);
            }
            case FOUL -> {
                Player fouler = PlayerPicker.pick(defenders, PlayerAction.COMMIT_FOUL, context.random());
                yield new StateResult(new FoulState(fouler, target, true), seconds);
            }
            case OWN_GOAL -> {
                Player unlucky = PlayerPicker.pick(defenders, PlayerAction.CLEAR_INTO_OWN_NET, context.random());
                context.recordGoal(attackers, unlucky, null, GoalEvent.GoalType.OWN_GOAL);
                context.switchPossession();
                yield new StateResult(new KickoffState(), seconds + EngineConfig.GOAL_CELEBRATION);
            }
        };
    }
}
