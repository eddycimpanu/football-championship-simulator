package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;

public class DispossessionState extends RestartState {

    @Override
    public StateResult execute(MatchContext context) {
        return new StateResult(new AttackState(),
                duration(context, EngineConfig.DISPOSSESSION_MIN, EngineConfig.DISPOSSESSION_MAX));
    }
}
