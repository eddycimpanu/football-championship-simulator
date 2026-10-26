package org.example.web;

import org.example.betting.MatchOdds;
import org.example.betting.OddsStore;
import org.example.persistence.MatchReportDao;
import org.example.persistence.MatchReportDao.EventLine;
import org.example.persistence.MatchReportDao.MatchInfo;
import org.example.persistence.MatchReportDao.PlayerLine;
import org.example.persistence.MatchReportDao.TeamStatsLine;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

// GET /api/matches/{id}: everything about one match, read from the database.
// All the events come back (each marked key or not), so the page can switch between them without asking again.
@RestController
@RequestMapping("/api")
public class MatchController {

    // odds = the pre-match odds, when the round was played from the web (null otherwise)
    public record MatchResponse(MatchInfo match, List<EventLine> events, TeamStatsLine homeStats,
                                TeamStatsLine awayStats, List<PlayerLine> homePlayers, List<PlayerLine> awayPlayers,
                                String manOfTheMatch, MatchOdds odds) {
    }

    private final MatchReportDao reportDao;
    private final OddsStore oddsStore;

    public MatchController(MatchReportDao reportDao, OddsStore oddsStore) {
        this.reportDao = reportDao;
        this.oddsStore = oddsStore;
    }

    @GetMapping("/matches/{id}")
    public MatchResponse match(@PathVariable int id) {
        MatchInfo match = reportDao.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no match with id " + id));
        MatchOdds odds = oddsStore.findByMatchIds(List.of(id)).get(id);
        if (!match.played()) {
            return new MatchResponse(match, List.of(), null, null, List.of(), List.of(), null, odds);
        }

        List<TeamStatsLine> stats = reportDao.findTeamStats(id);   // home team first
        List<PlayerLine> players = reportDao.findPlayerLines(id);
        List<PlayerLine> home = players.stream().filter(p -> p.team().equals(match.homeTeam())).toList();
        List<PlayerLine> away = players.stream().filter(p -> p.team().equals(match.awayTeam())).toList();

        return new MatchResponse(match, reportDao.findEvents(id, false),
                stats.isEmpty() ? null : stats.get(0), stats.size() < 2 ? null : stats.get(1),
                home, away, manOfTheMatch(players), odds);
    }

    // Best rating; with equal ratings, more goals and assists win (the same rule as the engine)
    private static String manOfTheMatch(List<PlayerLine> players) {
        return players.stream()
                .filter(p -> p.rating() != null)
                .max(Comparator.comparingDouble(PlayerLine::rating).thenComparingInt(p -> p.goals() + p.assists()))
                .map(PlayerLine::player)
                .orElse(null);
    }
}
