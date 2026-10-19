package org.example.model;

public record PlayerAttributes(
        int pace,
        int shooting,
        int passing,
        int dribbling,
        int defending,
        int physical,
        int heading,
        int reflexes,
        int handling,
        int stamina,
        int aggression
) {
    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 99;

    public PlayerAttributes {
        validate("pace", pace);
        validate("shooting", shooting);
        validate("passing", passing);
        validate("dribbling", dribbling);
        validate("defending", defending);
        validate("physical", physical);
        validate("heading", heading);
        validate("reflexes", reflexes);
        validate("handling", handling);
        validate("stamina", stamina);
        validate("aggression", aggression);
    }

    private static void validate(String name, int value) {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException(
                    name + " must be between " + MIN_VALUE + " and " + MAX_VALUE + ", got " + value);
        }
    }
}