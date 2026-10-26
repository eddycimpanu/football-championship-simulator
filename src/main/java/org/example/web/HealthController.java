package org.example.web;

import org.example.league.Season;
import org.example.league.SeasonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /api/health: shows that the server is up and has loaded the season from the database.
@RestController
@RequestMapping("/api")
public class HealthController {

    // Spring turns this record into JSON: {"status":"UP","season":"All-Star League 2026/27",...}
    public record HealthResponse(String status, String season, int seasonId, Integer nextRound, int rounds) {
    }

    private final SeasonService seasonService;

    // Constructor injection: Spring passes the SeasonService bean from AppConfig
    public HealthController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        Season season = seasonService.getSeason();
        Integer nextRound = season.isFinished() ? null : season.getCurrentRoundNumber();
        return new HealthResponse("UP", season.getName(), season.getId(), nextRound, season.getNumberOfRounds());
    }
}
