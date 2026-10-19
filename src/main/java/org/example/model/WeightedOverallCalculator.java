package org.example.model;

import java.util.EnumMap;
import java.util.Map;

public class WeightedOverallCalculator implements OverallCalculator {

    private static final int MAX_OVERALL = 99;

    private final Map<Attribute, Integer> weights;
    private final int bonus;

    public WeightedOverallCalculator(int bonus, Map<Attribute, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total != TOTAL_WEIGHT) {
            throw new IllegalArgumentException("Weights must add up to " + TOTAL_WEIGHT + ", got " + total);
        }
        this.weights = new EnumMap<>(weights);
        this.bonus = bonus;
    }

    @Override
    public int calculate(PlayerAttributes attributes) {
        int weightedSum = 0;
        for (Map.Entry<Attribute, Integer> entry : weights.entrySet()) {
            weightedSum += attributes.get(entry.getKey()) * entry.getValue();
        }
        int overall = (weightedSum + TOTAL_WEIGHT / 2) / TOTAL_WEIGHT + bonus;
        return Math.min(MAX_OVERALL, overall);
    }
}