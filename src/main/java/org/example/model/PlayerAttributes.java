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

    public int get(Attribute attribute) {
        return switch (attribute) {
            case PACE -> pace;
            case SHOOTING -> shooting;
            case PASSING -> passing;
            case DRIBBLING -> dribbling;
            case DEFENDING -> defending;
            case PHYSICAL -> physical;
            case HEADING -> heading;
            case REFLEXES -> reflexes;
            case HANDLING -> handling;
            case STAMINA -> stamina;
            case AGGRESSION -> aggression;
        };
    }

    // A tired player: every skill is multiplied by the factor (stamina and aggression stay the same).
    public PlayerAttributes withFatigue(double factor) {
        if (factor >= 1) {
            return this;
        }
        return new PlayerAttributes(
                scale(pace, factor), scale(shooting, factor), scale(passing, factor),
                scale(dribbling, factor), scale(defending, factor), scale(physical, factor),
                scale(heading, factor), scale(reflexes, factor), scale(handling, factor),
                stamina, aggression);
    }

    private static int scale(int value, double factor) {
        return Math.max(MIN_VALUE, (int) Math.round(value * factor));
    }
}
