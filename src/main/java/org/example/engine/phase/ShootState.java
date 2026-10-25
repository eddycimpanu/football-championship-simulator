package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;
import org.example.engine.MatchTeam;
import org.example.engine.PlayerAction;
import org.example.engine.PlayerPicker;
import org.example.engine.ShotOutcome;
import org.example.engine.random.RouletteWheel;
import org.example.engine.random.SkillFactor;
import org.example.model.Player;

public class ShootState extends PhaseState {

    private enum SaveOutcome { REBOUND_TO_ATTACKER, PARRIED_BEHIND, CLEARED_TO_OPPONENT }

    private final ShotType shotType;
    private final Player shooter;
    private final Player assist;

    public ShootState(ShotType shotType, Player shooter, Player assist) {
        this.shotType = shotType;
        this.shooter = shooter;
        this.assist = assist;
    }

    @Override
    public StateResult execute(MatchContext context) {
        MatchTeam attackers = context.attackingTeam();
        MatchTeam defenders = context.defendingTeam();
        Player keeper = defenders.getGoalkeeper();
        int seconds = duration(context, EngineConfig.SHOT_MIN, EngineConfig.SHOT_MAX);

        // SHOOTING (HEADING for a header) vs the keeper's REFLEXES decides goal against save
        int finishing = shotType == ShotType.HEADER ? context.attributes(shooter).heading() : context.attributes(shooter).shooting();
        double vsKeeper = SkillFactor.of(finishing, context.attributes(keeper).reflexes()) * context.homeFactor();

        // In open play the nearest defender puts pressure on the shot
        double vsDefender = 1;
        if (shotType.hasDefenderPressure()) {
            Player defender = PlayerPicker.pick(defenders, PlayerAction.DEFEND, context.random());
            vsDefender = SkillFactor.of(finishing, context.attributes(defender).defending());
        }

        ShotOutcome outcome = new RouletteWheel<ShotOutcome>()
                .addFavourable(ShotOutcome.GOAL, shotType.goalWeight(), vsKeeper)
                .addOption(ShotOutcome.SAVE, shotType.saveWeight())
                .addUnfavourable(ShotOutcome.MISS, shotType.missWeight(), vsDefender)
                .spin(context.random());

        context.recordShot(attackers, shooter, outcome != ShotOutcome.MISS);

        return switch (outcome) {
            case GOAL -> {
                context.recordGoal(attackers, shooter, assist, shotType.goalType());
                context.switchPossession();
                yield new StateResult(new KickoffState(), seconds + EngineConfig.GOAL_CELEBRATION);
            }
            case MISS -> {
                context.switchPossession();
                yield new StateResult(new GoalKickState(), seconds);
            }
            case SAVE -> {
                context.recordSave(defenders, keeper, shooter);
                yield afterSave(context, keeper, seconds);
            }
        };
    }

    // What happens to the rebound? The keeper's HANDLING decides whether he holds the ball.
    private StateResult afterSave(MatchContext context, Player keeper, int seconds) {
        double handling = SkillFactor.of(context.attributes(keeper).handling(), EngineConfig.REFERENCE_ATTRIBUTE);

        SaveOutcome outcome = new RouletteWheel<SaveOutcome>()
                .addUnfavourable(SaveOutcome.REBOUND_TO_ATTACKER, EngineConfig.SAVE_REBOUND_TO_ATTACKER, handling)
                .addOption(SaveOutcome.PARRIED_BEHIND, EngineConfig.SAVE_PARRIED_BEHIND)
                .addFavourable(SaveOutcome.CLEARED_TO_OPPONENT, EngineConfig.SAVE_CLEARED, handling)
                .spin(context.random());

        return switch (outcome) {
            case REBOUND_TO_ATTACKER -> new StateResult(new AttackState(), seconds);
            case PARRIED_BEHIND -> new StateResult(new CornerState(), seconds);
            case CLEARED_TO_OPPONENT -> {
                context.switchPossession();
                yield new StateResult(new DispossessionState(), seconds);
            }
        };
    }
}
