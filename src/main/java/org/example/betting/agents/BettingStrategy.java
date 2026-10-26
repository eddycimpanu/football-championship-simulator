package org.example.betting.agents;

import java.math.BigDecimal;
import java.util.List;

// How an agent bets (Strategy pattern): each agent decides with its own rules, then its tickets go
// through the same BettingService as yours, so an agent cannot break the betting rules.
public interface BettingStrategy {

    // A short line for the page, e.g. "Backs the biggest favourite of the round"
    String description();

    // The tickets for the next round (may be none)
    List<TicketPlan> decide(List<MatchView> matches, BigDecimal balance);
}
