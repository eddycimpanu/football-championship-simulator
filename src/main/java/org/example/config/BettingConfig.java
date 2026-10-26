package org.example.config;

public final class BettingConfig {

    private BettingConfig() {
        // constants only, no instances
    }

    // ----- Odds -----
    public static final int ODDS_SIMULATIONS = 1000;   // every match is played this many times before the round
    public static final double ODDS_MARGIN = 0.05;     // the bookmaker's margin: chances of a market add up to 105%
    // Favourites pay less: a selection with a chance above 50% gets up to this much extra margin
    // (a 75% favourite: 5% + 5% = 10%, so 1.21 instead of 1.27). Underdogs keep the normal margin.
    public static final double FAVOURITE_EXTRA_MARGIN = 0.10;

    // ----- Bets -----
    public static final java.math.BigDecimal STARTING_BALANCE = new java.math.BigDecimal("1000.00");  // every season
    public static final java.math.BigDecimal MIN_STAKE = new java.math.BigDecimal("1.00");
    public static final String HUMAN_BETTOR = "You";
}
