package org.example.betting.agents;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Stakes {

    private static final BigDecimal ONE_CREDIT = BigDecimal.ONE.setScale(2);

    private Stakes() {
    }

    // A share of the balance, rounded down to the cent; null when it would be under 1 credit
    static BigDecimal share(BigDecimal balance, double share) {
        BigDecimal stake = balance.multiply(BigDecimal.valueOf(share)).setScale(2, RoundingMode.DOWN);
        return stake.compareTo(ONE_CREDIT) < 0 ? null : stake;
    }

    static BigDecimal fixed(BigDecimal balance, String amount) {
        BigDecimal stake = new BigDecimal(amount).setScale(2);
        return balance.compareTo(stake) < 0 ? null : stake;
    }
}
