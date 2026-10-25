package org.example.engine.phase;

import org.example.engine.MatchContext;

public abstract class PhaseState {

    // Runs this state once: makes its decision with the roulette wheel and says where the ball goes next.
    public abstract StateResult execute(MatchContext context);

    // Restart states (kickoff, corner, throw-in...) start a new phase.
    public boolean isRestart() {
        return false;
    }

    public String getName() {
        return getClass().getSimpleName().replace("State", "");
    }

    protected static int duration(MatchContext context, int minSeconds, int maxSeconds) {
        return context.random().nextInt(minSeconds, maxSeconds);
    }

    @Override
    public String toString() {
        return getName();
    }
}
