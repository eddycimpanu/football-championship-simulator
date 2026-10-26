package org.example.betting.agents;

import org.example.betting.MatchOdds;

// One match of the next round as an agent sees it: the two teams' form and the bookmaker's odds.
public record MatchView(int matchId, String homeTeam, String awayTeam, TeamForm home, TeamForm away, MatchOdds odds) {
}
