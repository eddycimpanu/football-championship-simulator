package org.example.model;

public class DefenderOverallCalculator implements OverallCalculator {

    private static final int PACE = 15;
    private static final int PASSING = 10;
    private static final int DEFENDING = 40;
    private static final int PHYSICAL = 20;
    private static final int HEADING = 15;

    @Override
    public int calculate(PlayerAttributes a) {
        int weightedSum = a.pace() * PACE
                + a.passing() * PASSING
                + a.defending() * DEFENDING
                + a.physical() * PHYSICAL
                + a.heading() * HEADING;
        return Math.round(weightedSum / (float) TOTAL_WEIGHT);
    }
}