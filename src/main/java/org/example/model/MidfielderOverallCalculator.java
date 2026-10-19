package org.example.model;

public class MidfielderOverallCalculator implements OverallCalculator {

    private static final int SHOOTING = 10;
    private static final int PASSING = 40;
    private static final int DRIBBLING = 25;
    private static final int DEFENDING = 15;
    private static final int PHYSICAL = 10;

    @Override
    public int calculate(PlayerAttributes a) {
        int weightedSum = a.shooting() * SHOOTING
                + a.passing() * PASSING
                + a.dribbling() * DRIBBLING
                + a.defending() * DEFENDING
                + a.physical() * PHYSICAL;
        return Math.round(weightedSum / (float) TOTAL_WEIGHT);
    }
}