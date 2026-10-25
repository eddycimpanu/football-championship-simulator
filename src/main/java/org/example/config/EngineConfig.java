package org.example.config;

public final class EngineConfig {

    private EngineConfig() {
        // constants only, no instances
    }

    // Stoppage time: base + per event, then kept between a minimum and a maximum
    public static final double STOPPAGE_BASE_MINUTES = 1.0;
    public static final double STOPPAGE_PER_EVENT = 0.5;      // per goal, substitution or card
    public static final double STOPPAGE_PER_INJURY = 1.0;
    public static final int MIN_STOPPAGE_FIRST_HALF = 1;
    public static final int MAX_STOPPAGE_FIRST_HALF = 5;
    public static final int MIN_STOPPAGE_SECOND_HALF = 2;
    public static final int MAX_STOPPAGE_SECOND_HALF = 8;
}