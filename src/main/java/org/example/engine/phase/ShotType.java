package org.example.engine.phase;

import org.example.engine.event.GoalEvent;

public enum ShotType {
    //            goal  save  miss   pressure from a defender?
    OPEN_PLAY(     10,   24,   66,   true,  GoalEvent.GoalType.NORMAL),
    HEADER(        11,   22,   67,   true,  GoalEvent.GoalType.HEADER),
    FREE_KICK(      7,   30,   63,   false, GoalEvent.GoalType.FREE_KICK),
    PENALTY(       76,   18,    6,   false, GoalEvent.GoalType.PENALTY);

    private final double goalWeight;
    private final double saveWeight;
    private final double missWeight;
    private final boolean defenderPressure;
    private final GoalEvent.GoalType goalType;

    ShotType(double goalWeight, double saveWeight, double missWeight, boolean defenderPressure,
             GoalEvent.GoalType goalType) {
        this.goalWeight = goalWeight;
        this.saveWeight = saveWeight;
        this.missWeight = missWeight;
        this.defenderPressure = defenderPressure;
        this.goalType = goalType;
    }

    public double goalWeight() { return goalWeight; }
    public double saveWeight() { return saveWeight; }
    public double missWeight() { return missWeight; }
    public boolean hasDefenderPressure() { return defenderPressure; }
    public GoalEvent.GoalType goalType() { return goalType; }
}
