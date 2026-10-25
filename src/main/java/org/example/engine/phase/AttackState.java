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

public class AttackState extends PhaseState {

    private enum Outcome {
        SHOT_TAKEN, DEFENDER_WINS_BALL, FOUL, OFFSIDE,
        OUT_FOR_THROW_IN, OUT_FOR_CORNER, OUT_FOR_GOAL_KICK, OWN_GOAL
    }

    @Override
    public StateResult execute(MatchContext context) {
        MatchTeam attackers = context.attackingTeam();
        MatchTeam defenders = context.defendingTeam();
        int seconds = duration(context, EngineConfig.ATTACK_MIN, EngineConfig.ATTACK_MAX);

        // Team attack vs team defence, plus home advantage
        double skill = SkillFactor.of(attackers.getAttackRating(), defenders.getDefenceRating()) * context.matchupFactor();
        // Aggressive defenders commit more fouls
        double aggression = defenders.averageOf(Attribute.AGGRESSION, FieldRole.DEFENDER, FieldRole.MIDFIELDER)
                / EngineConfig.REFERENCE_ATTRIBUTE;

        Outcome outcome = new RouletteWheel<Outcome>()
                .addFavourable(Outcome.SHOT_TAKEN, EngineConfig.ATTACK_SHOT, skill)
                .addUnfavourable(Outcome.DEFENDER_WINS_BALL, EngineConfig.ATTACK_DEFENDER_WINS_BALL, skill)
                .addOption(Outcome.FOUL, EngineConfig.ATTACK_FOUL * aggression)
                .addOption(Outcome.OFFSIDE, EngineConfig.ATTACK_OFFSIDE)
                .addOption(Outcome.OUT_FOR_THROW_IN, EngineConfig.ATTACK_OUT_FOR_THROW_IN)
                .addFavourable(Outcome.OUT_FOR_CORNER, EngineConfig.ATTACK_OUT_FOR_CORNER, skill)
                .addOption(Outcome.OUT_FOR_GOAL_KICK, EngineConfig.ATTACK_OUT_FOR_GOAL_KICK)
                .addOption(Outcome.OWN_GOAL, EngineConfig.ATTACK_OWN_GOAL)
                .spin(context.random());

        return switch (outcome) {
            case SHOT_TAKEN -> {
                Player shooter = PlayerPicker.pick(attackers, PlayerAction.SHOOT, context.random());
                Player assist = context.random().chance(EngineConfig.ASSIST_CHANCE)
                        ? PlayerPicker.pick(attackers, PlayerAction.ASSIST, shooter, context.random())
                        : null;
                if (assist != null) {
                    context.recordKeyPass(assist);
                }
                yield new StateResult(new ShootState(ShotType.OPEN_PLAY, shooter, assist), seconds);
            }
            case DEFENDER_WINS_BALL -> {
                context.recordTackle(PlayerPicker.pick(defenders, PlayerAction.DEFEND, context.random()));
                context.switchPossession();
                yield new StateResult(new DispossessionState(), seconds);
            }
            case FOUL -> {
                Player fouler = PlayerPicker.pick(defenders, PlayerAction.COMMIT_FOUL, context.random());
                Player fouled = PlayerPicker.pick(attackers, PlayerAction.WIN_FOUL, context.random());
                yield new StateResult(new FoulState(fouler, fouled, false), seconds);
            }
            case OFFSIDE -> {
                Player runner = PlayerPicker.pick(attackers, PlayerAction.RUN_IN_BEHIND, context.random());
                context.recordOffside(attackers, runner);
                context.switchPossession();
                yield new StateResult(new FreeKickState(false), seconds);
            }
            case OUT_FOR_THROW_IN -> {
                if (context.random().chance(EngineConfig.THROW_IN_TO_OPPONENT)) {
                    context.switchPossession();
                }
                yield new StateResult(new ThrowInState(), seconds);
            }
            case OUT_FOR_CORNER -> new StateResult(new CornerState(), seconds);
            case OUT_FOR_GOAL_KICK -> {
                context.switchPossession();
                yield new StateResult(new GoalKickState(), seconds);
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
