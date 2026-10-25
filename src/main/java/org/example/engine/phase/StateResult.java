package org.example.engine.phase;

public record StateResult(PhaseState next, int seconds) {

    public StateResult {
        if (next == null) {
            throw new IllegalArgumentException("Every state must say what comes next");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("A state cannot take negative time");
        }
    }
}
