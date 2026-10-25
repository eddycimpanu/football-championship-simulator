package org.example.engine.phase;

import org.example.config.EngineConfig;
import org.example.engine.MatchContext;

public class GoalKickState extends RestartState {

    @Override
    public StateResult execute(MatchContext context) {
        return new StateResult(new AttackState(), duration(context, EngineConfig.GOAL_KICK_MIN, EngineConfig.GOAL_KICK_MAX));
    }
}
