package org.example.model;

public class ForwardOverallCalculator implements OverallCalculator {

    private static final int PACE = 20;
    private static final int SHOOTING = 40;
    private static final int PASSING = 5;
    private static final int DRIBBLING = 20;
    private static final int PHYSICAL = 5;
    private static final int HEADING = 10;

    @Override
    public int calculate(PlayerAttributes a) {
        int weightedSum = a.pace() * PACE
                + a.shooting() * SHOOTING
                + a.passing() * PASSING
                + a.dribbling() * DRIBBLING
                + a.physical() * PHYSICAL
                + a.heading() * HEADING;
        return Math.round(weightedSum / (float) TOTAL_WEIGHT);
    }
}