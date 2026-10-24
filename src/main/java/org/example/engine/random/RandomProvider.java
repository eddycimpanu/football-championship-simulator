package org.example.engine.random;

import java.util.List;
import java.util.Random;

public class RandomProvider {

    private final long seed;
    private final Random random;

    public RandomProvider(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public static RandomProvider withRandomSeed() {
        return new RandomProvider(System.nanoTime());
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    public int nextInt(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("min must not be greater than max");
        }
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    public boolean chance(double probability) {
        if (probability < 0 || probability > 1) {
            throw new IllegalArgumentException("Probability must be between 0 and 1, got " + probability);
        }
        return random.nextDouble() < probability;
    }

    public <T> T pickOne(List<T> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cannot pick from an empty list");
        }
        return items.get(random.nextInt(items.size()));
    }

    public long getSeed() {
        return seed;
    }
}