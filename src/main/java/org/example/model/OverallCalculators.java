package org.example.model;

import java.util.EnumMap;
import java.util.Map;

import static org.example.model.Attribute.*;

public final class OverallCalculators {

    private static final OverallCalculator GOALKEEPER = new WeightedOverallCalculator(1,
            Map.of(REFLEXES, 55, HANDLING, 35, PASSING, 5, PHYSICAL, 5));

    private static final OverallCalculator CENTRE_BACK = new WeightedOverallCalculator(3,
            Map.of(DEFENDING, 55, PHYSICAL, 15, HEADING, 15, PACE, 10, PASSING, 5));

    private static final OverallCalculator FULL_BACK = new WeightedOverallCalculator(5,
            Map.of(DEFENDING, 35, PACE, 25, PASSING, 20, DRIBBLING, 10, STAMINA, 10));

    private static final OverallCalculator DEFENSIVE_MIDFIELDER = new WeightedOverallCalculator(5,
            Map.of(DEFENDING, 40, PASSING, 30, PHYSICAL, 15, STAMINA, 10, AGGRESSION, 5));

    private static final OverallCalculator CENTRAL_MIDFIELDER = new WeightedOverallCalculator(5,
            Map.of(PASSING, 45, DRIBBLING, 20, SHOOTING, 10, DEFENDING, 10, STAMINA, 10, PHYSICAL, 5));

    private static final OverallCalculator ATTACKING_MIDFIELDER = new WeightedOverallCalculator(2,
            Map.of(PASSING, 35, DRIBBLING, 35, SHOOTING, 25, PACE, 5));

    private static final OverallCalculator WIDE_MIDFIELDER = new WeightedOverallCalculator(3,
            Map.of(PASSING, 30, DRIBBLING, 30, PACE, 20, SHOOTING, 10, STAMINA, 10));

    private static final OverallCalculator WINGER = new WeightedOverallCalculator(1,
            Map.of(DRIBBLING, 35, PACE, 30, SHOOTING, 25, PASSING, 10));

    private static final OverallCalculator STRIKER = new WeightedOverallCalculator(4,
            Map.of(SHOOTING, 50, DRIBBLING, 15, PACE, 15, HEADING, 10, PHYSICAL, 10));

    private static final Map<DetailedPosition, OverallCalculator> CALCULATORS = new EnumMap<>(DetailedPosition.class);

    static {
        CALCULATORS.put(DetailedPosition.GK, GOALKEEPER);

        CALCULATORS.put(DetailedPosition.CB, CENTRE_BACK);
        CALCULATORS.put(DetailedPosition.LB, FULL_BACK);
        CALCULATORS.put(DetailedPosition.RB, FULL_BACK);
        CALCULATORS.put(DetailedPosition.LWB, FULL_BACK);
        CALCULATORS.put(DetailedPosition.RWB, FULL_BACK);

        CALCULATORS.put(DetailedPosition.CDM, DEFENSIVE_MIDFIELDER);
        CALCULATORS.put(DetailedPosition.CM, CENTRAL_MIDFIELDER);
        CALCULATORS.put(DetailedPosition.CAM, ATTACKING_MIDFIELDER);
        CALCULATORS.put(DetailedPosition.LM, WIDE_MIDFIELDER);
        CALCULATORS.put(DetailedPosition.RM, WIDE_MIDFIELDER);

        CALCULATORS.put(DetailedPosition.LW, WINGER);
        CALCULATORS.put(DetailedPosition.RW, WINGER);
        CALCULATORS.put(DetailedPosition.CF, STRIKER);
        CALCULATORS.put(DetailedPosition.ST, STRIKER);
    }

    private OverallCalculators() {
        // utility class, no instances
    }

    public static OverallCalculator forPosition(DetailedPosition position) {
        OverallCalculator calculator = CALCULATORS.get(position);
        if (calculator == null) {
            throw new IllegalArgumentException("No overall calculator for " + position);
        }
        return calculator;
    }
}