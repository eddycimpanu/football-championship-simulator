package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;

public class ThrowInState extends RestartState {

    @Override
    public StateResult execute(MatchContext context) {
        return new StateResult(new AttackState(), duration(context, EngineConfig.THROW_IN_MIN, EngineConfig.THROW_IN_MAX));
    }
}
