package org.example.engine.random;

public final class SkillFactor {

    private SkillFactor() {
        // utility class, no instances
    }

    public static double of(double attacker, double defender) {
        if (attacker <= 0 || defender <= 0) {
            throw new IllegalArgumentException("Attributes must be positive");
        }
        double ratio = attacker / defender;
        return ratio * ratio;
    }
}