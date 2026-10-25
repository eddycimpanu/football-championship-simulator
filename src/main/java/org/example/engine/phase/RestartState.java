package org.example.engine.phase;

public abstract class RestartState extends PhaseState {

    @Override
    public final boolean isRestart() {
        return true;
    }
}
