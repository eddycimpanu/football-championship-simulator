package org.example.betting;

import java.math.BigDecimal;

// One selection on a ticket, with its match. The goals are null until the match is played.
public record TicketSelection(int id, int matchId, int round, String homeTeam, String homeShort, String awayTeam,
                              String awayShort, Integer homeGoals, Integer awayGoals, Selection selection,
                              BigDecimal odds, BetStatus status) {
}
