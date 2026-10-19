package org.example.model;

public class GoalkeeperOverallCalculator implements OverallCalculator {

    private static final int PASSING = 5;
    private static final int PHYSICAL = 10;
    private static final int REFLEXES = 50;
    private static final int HANDLING = 35;

    @Override
    public int calculate(PlayerAttributes a) {
        int weightedSum = a.passing() * PASSING
                + a.physical() * PHYSICAL
                + a.reflexes() * REFLEXES
                + a.handling() * HANDLING;
        return Math.round(weightedSum / (float) TOTAL_WEIGHT);
    }
}