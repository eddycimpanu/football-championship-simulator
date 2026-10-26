package org.example.engine.random;

public final class SkillFactor {
    private SkillFactor() { }
    public static double of(double attacker, double defender) {
        if (attacker <= 0 || defender <= 0) throw new IllegalArgumentException("Attributes must be positive");
        double ratio = attacker / defender;
        return ratio * ratio;
    }

    // Same idea with a stronger exponent: used for team against team, where the ratings are close together
    public static double of(double attacker, double defender, double exponent) {
        if (attacker <= 0 || defender <= 0) throw new IllegalArgumentException("Attributes must be positive");
        return Math.pow(attacker / defender, exponent);
    }
}
