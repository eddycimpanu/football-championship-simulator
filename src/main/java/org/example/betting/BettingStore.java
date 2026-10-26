package org.example.betting;

import java.math.BigDecimal;
import java.util.List;

// Where bettors and tickets are kept. The betting rules only know this interface; the database code implements it.
public interface BettingStore {

    // The bettor with this name in the season; created with the starting balance the first time
    Bettor findOrCreateBettor(int seasonId, String name, String kind, String strategy, BigDecimal startingBalance);

    List<Bettor> findBettors(int seasonId);

    // Newest first
    List<Ticket> findTickets(int bettorId);

    // Takes the stake from the balance and saves the ticket with its selections, all or nothing.
    // Throws BettingException when the balance is too small.
    Ticket placeTicket(int bettorId, int round, BigDecimal stake, BigDecimal totalOdds, List<PricedPick> picks);

    // Settles the selections of every played match and pays the tickets that are complete; returns them
    List<Ticket> settlePlayedTickets(int seasonId);

    // A pick with the odds the server found for it
    record PricedPick(int matchId, Selection selection, BigDecimal odds) {
    }
}
