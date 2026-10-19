package org.example.model;

import java.util.EnumMap;
import java.util.Map;

public final class OverallCalculators {

    private static final Map<FieldRole, OverallCalculator> CALCULATORS = new EnumMap<>(FieldRole.class);

    static {
        CALCULATORS.put(FieldRole.GOALKEEPER, new GoalkeeperOverallCalculator());
        CALCULATORS.put(FieldRole.DEFENDER, new DefenderOverallCalculator());
        CALCULATORS.put(FieldRole.MIDFIELDER, new MidfielderOverallCalculator());
        CALCULATORS.put(FieldRole.FORWARD, new ForwardOverallCalculator());
    }

    private OverallCalculators() {
        // utility class, no instances
    }

    public static OverallCalculator forRole(FieldRole fieldRole) {
        OverallCalculator calculator = CALCULATORS.get(fieldRole);
        if (calculator == null) {
            throw new IllegalArgumentException("No overall calculator for " + fieldRole);
        }
        return calculator;
    }
}