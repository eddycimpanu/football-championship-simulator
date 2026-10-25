package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;
import org.example.engine.MatchTeam;
import org.example.engine.PlayerAction;
import org.example.engine.PlayerPicker;
import org.example.engine.event.CornerEvent;
import org.example.engine.random.RouletteWheel;
import org.example.engine.random.SkillFactor;
import org.example.model.Attribute;
import org.example.model.FieldRole;

public class CornerState extends RestartState {

    private enum Choice { CROSS, SHORT_PASS }

    @Override
    public StateResult execute(MatchContext context) {
        MatchTeam attackers = context.attackingTeam();
        context.addEvent(new CornerEvent(context.minute(), attackers.getTeam()));
        int seconds = duration(context, EngineConfig.CORNER_MIN, EngineConfig.CORNER_MAX);

        // Good headers in the box: cross it. Good passers in midfield: play it short.
        double aerialSkill = SkillFactor.of(attackers.averageOfTop(Attribute.HEADING, 3), EngineConfig.REFERENCE_ATTRIBUTE);
        double passingSkill = SkillFactor.of(attackers.averageOf(Attribute.PASSING, FieldRole.MIDFIELDER),
                EngineConfig.REFERENCE_ATTRIBUTE);

        Choice choice = new RouletteWheel<Choice>()
                .addFavourable(Choice.CROSS, EngineConfig.CORNER_CROSS, aerialSkill)
                .addFavourable(Choice.SHORT_PASS, EngineConfig.CORNER_SHORT_PASS, passingSkill)
                .spin(context.random());

        if (choice == Choice.CROSS) {
            return new StateResult(new CrossState(
                    PlayerPicker.pick(attackers.getOnPitch(), PlayerAction.CROSS, context.random())), seconds);
        }
        return new StateResult(new AttackState(), seconds);
    }
}
