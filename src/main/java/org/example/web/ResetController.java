package org.example.web;

import org.example.league.LeagueSource;
import org.example.league.Season;
import org.example.league.SeasonService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// POST /api/season/reset: a new season from round 1, everyone back to the starting credits.
// The old season, its matches and its tickets stay in the database.
@RestController
@RequestMapping("/api")
public class ResetController {

    public record ResetResponse(int seasonId, String name, int totalRounds) {
    }

    private final SeasonService seasonService;
    private final LeagueSource league;

    public ResetController(SeasonService seasonService, LeagueSource league) {
        this.seasonService = seasonService;
        this.league = league;
    }

    @PostMapping("/season/reset")
    public ResetResponse reset() {
        synchronized (seasonService) {
            seasonService.restart(league.loadTeams(), league.loadReferees(), System.nanoTime());
            Season season = seasonService.getSeason();
            return new ResetResponse(season.getId(), season.getName(), season.getNumberOfRounds());
        }
    }
}
