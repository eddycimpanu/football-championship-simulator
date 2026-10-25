package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;
import org.example.engine.MatchTeam;
import org.example.engine.PlayerAction;
import org.example.engine.PlayerPicker;
import org.example.engine.random.RouletteWheel;
import org.example.engine.random.SkillFactor;
import org.example.model.Attribute;
import org.example.model.Player;

public class FreeKickState extends RestartState {

    private enum Choice { SHOOT, CROSS, SHORT_PASS }

    // Dangerous = close to goal (after a foul). Not dangerous = far away (after an offside).
    private final boolean dangerous;

    public FreeKickState(boolean dangerous) {
        this.dangerous = dangerous;
    }

    @Override
    public StateResult execute(MatchContext context) {
        MatchTeam attackers = context.attackingTeam();
        int seconds = duration(context, EngineConfig.FREE_KICK_MIN, EngineConfig.FREE_KICK_MAX);
        Player taker = PlayerPicker.best(attackers, Attribute.SHOOTING);

        RouletteWheel<Choice> wheel = new RouletteWheel<>();
        if (dangerous) {
            // A great free-kick taker shoots more; good headers in the box make a cross worth it
            double takerSkill = SkillFactor.of(context.attributes(taker).shooting(), EngineConfig.REFERENCE_ATTRIBUTE);
            double aerialSkill = SkillFactor.of(attackers.averageOfTop(Attribute.HEADING, 3), EngineConfig.REFERENCE_ATTRIBUTE);
            wheel.addFavourable(Choice.SHOOT, EngineConfig.FREE_KICK_SHOOT, takerSkill)
                    .addFavourable(Choice.CROSS, EngineConfig.FREE_KICK_CROSS, aerialSkill)
                    .addOption(Choice.SHORT_PASS, EngineConfig.FREE_KICK_SHORT_PASS);
        } else {
            wheel.addOption(Choice.CROSS, EngineConfig.DEEP_FREE_KICK_CROSS)
                    .addOption(Choice.SHORT_PASS, EngineConfig.DEEP_FREE_KICK_SHORT_PASS);
        }

        return switch (wheel.spin(context.random())) {
            case SHOOT -> new StateResult(new ShootState(ShotType.FREE_KICK, taker, null), seconds);
            case CROSS -> {
                Player crosser = PlayerPicker.pick(attackers, PlayerAction.CROSS, context.random());
                yield new StateResult(new CrossState(crosser), seconds);
            }
            case SHORT_PASS -> new StateResult(new AttackState(), seconds);
        };
    }
}
