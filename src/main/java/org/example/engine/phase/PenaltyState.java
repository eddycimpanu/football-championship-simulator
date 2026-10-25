package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;
import org.example.engine.PlayerPicker;
import org.example.model.Attribute;
import org.example.model.Player;

public class PenaltyState extends PhaseState {

    @Override
    public StateResult execute(MatchContext context) {
        Player taker = PlayerPicker.best(context.attackingTeam().getOutfieldPlayers(), Attribute.SHOOTING);
        return new StateResult(new ShootState(ShotType.PENALTY, taker, null),
                duration(context, EngineConfig.PENALTY_MIN, EngineConfig.PENALTY_MAX));
    }
}
