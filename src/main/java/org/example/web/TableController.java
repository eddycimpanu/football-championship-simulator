package org.example.web;

import org.example.league.Fixture;
import org.example.league.LeagueTable;
import org.example.league.Round;
import org.example.league.Season;
import org.example.league.SeasonService;
import org.example.league.StandingRow;
import org.example.model.Team;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// GET /api/table: the league table after the last round played, with each team's movement since the round before.
@RestController
@RequestMapping("/api")
public class TableController {

    public record TableRow(int position, Integer previousPosition, int teamId, String team, String shortName,
                           int played, int won, int drawn, int lost, int goalsFor, int goalsAgainst,
                           int goalDifference, int points, List<String> form) {
    }

    public record TableResponse(String season, int roundsPlayed, int totalRounds, List<TableRow> rows) {
    }

    private final SeasonService seasonService;

    public TableController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping("/table")
    public TableResponse table() {
        // The season is not read while a round is being simulated (see SeasonController)
        synchronized (seasonService) {
            return buildTable(seasonService.getSeason());
        }
    }

    private TableResponse buildTable(Season season) {
        int roundsPlayed = season.getRoundsPlayed();

        // The table one round earlier tells how many places each team moved
        Map<Team, Integer> previousPositions = roundsPlayed >= 2
                ? positions(tableUpTo(season, roundsPlayed - 1))
                : Map.of();

        List<StandingRow> standings = season.getTable().getStandings();
        List<TableRow> rows = new ArrayList<>();
        for (int i = 0; i < standings.size(); i++) {
            StandingRow row = standings.get(i);
            Team team = row.getTeam();
            rows.add(new TableRow(i + 1, previousPositions.get(team), team.getId(), team.getName(), team.getShortName(),
                    row.getPlayed(), row.getWon(), row.getDrawn(), row.getLost(), row.getGoalsFor(),
                    row.getGoalsAgainst(), row.getGoalDifference(), row.getPoints(),
                    row.getForm().chars().mapToObj(c -> String.valueOf((char) c)).toList()));
        }
        return new TableResponse(season.getName(), roundsPlayed, season.getNumberOfRounds(), rows);
    }

    private static LeagueTable tableUpTo(Season season, int lastRound) {
        LeagueTable table = new LeagueTable(season.getTeams());
        for (Round round : season.getRounds()) {
            if (round.getNumber() > lastRound) {
                break;
            }
            for (Fixture fixture : round.getFixtures()) {
                if (fixture.isPlayed()) {
                    table.record(fixture.getHomeTeam(), fixture.getAwayTeam(),
                            fixture.getScore().home(), fixture.getScore().away());
                }
            }
        }
        return table;
    }

    private static Map<Team, Integer> positions(LeagueTable table) {
        Map<Team, Integer> positions = new HashMap<>();
        List<StandingRow> standings = table.getStandings();
        for (int i = 0; i < standings.size(); i++) {
            positions.put(standings.get(i).getTeam(), i + 1);
        }
        return positions;
    }
}
