package org.example.engine.random;

import java.util.ArrayList;
import java.util.List;

public class RouletteWheel<T> {

    private final List<Slice<T>> slices = new ArrayList<>();
    private double totalWeight;

    public RouletteWheel<T> addOption(T outcome, double weight) {
        if (outcome == null) {
            throw new IllegalArgumentException("Outcome cannot be null");
        }
        if (weight < 0 || Double.isNaN(weight) || Double.isInfinite(weight)) {
            throw new IllegalArgumentException("Weight must be zero or positive, got " + weight + " for " + outcome);
        }
        if (weight > 0) {
            slices.add(new Slice<>(outcome, weight));
            totalWeight += weight;
        }
        return this;
    }

    public RouletteWheel<T> addFavourable(T outcome, double baseWeight, double skillFactor) {
        return addOption(outcome, baseWeight * skillFactor);
    }

    public RouletteWheel<T> addUnfavourable(T outcome, double baseWeight, double skillFactor) {
        return addOption(outcome, baseWeight / skillFactor);
    }

    public T spin(RandomProvider random) {
        return pick(random.nextDouble());
    }

    public T pick(double roll) {
        if (slices.isEmpty()) {
            throw new IllegalStateException("The wheel has no options with a positive weight");
        }
        if (roll < 0 || roll >= 1) {
            throw new IllegalArgumentException("Roll must be in [0, 1), got " + roll);
        }
        double target = roll * totalWeight;
        double cumulative = 0;
        for (Slice<T> slice : slices) {
            cumulative += slice.weight();
            if (target < cumulative) {
                return slice.outcome();
            }
        }
        return slices.get(slices.size() - 1).outcome();
    }

    public double probabilityOf(T outcome) {
        if (totalWeight == 0) {
            return 0;
        }
        double weight = slices.stream()
                .filter(slice -> slice.outcome().equals(outcome))
                .mapToDouble(Slice::weight)
                .sum();
        return weight / totalWeight;
    }

    public String describe() {
        StringBuilder text = new StringBuilder();
        double from = 0;
        for (Slice<T> slice : slices) {
            double to = from + slice.weight() / totalWeight;
            text.append(String.format("  %-12s %.3f - %.3f%n", slice.outcome(), from, to));
            from = to;
        }
        return text.toString();
    }

    private record Slice<T>(T outcome, double weight) {
    }
}