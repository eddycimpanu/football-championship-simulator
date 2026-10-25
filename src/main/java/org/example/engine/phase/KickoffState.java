package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;

public class KickoffState extends RestartState {

    @Override
    public StateResult execute(MatchContext context) {
        return new StateResult(new AttackState(), duration(context, EngineConfig.KICKOFF_MIN, EngineConfig.KICKOFF_MAX));
    }
}
