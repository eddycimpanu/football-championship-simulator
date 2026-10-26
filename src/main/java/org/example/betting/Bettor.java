package org.example.betting;

import java.math.BigDecimal;

// Someone who bets in one season: you (HUMAN) or an AI agent (AGENT, with its strategy).
// Money is BigDecimal: 0.1 + 0.2 is exactly 0.3, which a double cannot promise.
public record Bettor(int id, int seasonId, String name, String kind, String strategy, BigDecimal startingBalance,
                     BigDecimal balance) {

    public BigDecimal profit() {
        return balance.subtract(startingBalance);
    }
}
