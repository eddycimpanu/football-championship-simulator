package org.example.web;

import org.example.league.Fixture;
import org.example.league.LeagueTable;
import org.example.league.Season;
import org.example.league.SeasonService;
import org.example.league.StandingRow;
import org.example.model.Player;
import org.example.model.Team;
import org.example.persistence.PlayerStatsDao;
import org.example.persistence.PlayerStatsDao.SquadLine;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// GET /api/teams and GET /api/teams/{id}
@RestController
@RequestMapping("/api")
public class TeamController {

    public record TeamSummary(int id, String name, String shortName, String city, String stadium, String coach,
                              String formation, int position, int points) {
    }

    public record Standing(int position, int played, int won, int drawn, int lost, int goalsFor, int goalsAgainst,
                           int goalDifference, int points, List<String> form) {
    }

    public record SquadPlayer(int id, String name, int shirtNumber, String position, String fieldRole, int overall,
                              int age, String nationality, String status, int appearances, int minutes, int goals,
                              int assists, Double averageRating) {
    }

    // Goals and result are null until the match is played; result is W, D or L for this team
    public record TeamFixture(int round, int matchId, boolean home, int opponentId, String opponent,
                              String opponentShort, Integer goalsFor, Integer goalsAgainst, String result) {
    }

    public record TeamProfile(int id, String name, String shortName, String city, String stadium, String coach,
                              String formation, Standing standing, List<SquadPlayer> squad,
                              List<TeamFixture> fixtures) {
    }

    private final SeasonService seasonService;
    private final PlayerStatsDao statsDao;

    public TeamController(SeasonService seasonService, PlayerStatsDao statsDao) {
        this.seasonService = seasonService;
        this.statsDao = statsDao;
    }

    @GetMapping("/teams")
    public List<TeamSummary> teams() {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            LeagueTable table = season.getTable();
            return season.getTeams().stream()
                    .map(team -> new TeamSummary(team.getId(), team.getName(), team.getShortName(), team.getCity(),
                            team.getStadium(), team.getCoach().getFullName(),
                            team.getCoach().getPreferredFormation().getDisplayName(),
                            table.positionOf(team), table.rowOf(team).getPoints()))
                    .sorted(Comparator.comparing(TeamSummary::name))
                    .toList();
        }
    }

    @GetMapping("/teams/{id}")
    public TeamProfile team(@PathVariable int id) {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            Team team = season.getTeams().stream()
                    .filter(t -> t.getId() == id)
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no team with id " + id));

            LeagueTable table = season.getTable();
            StandingRow row = table.rowOf(team);
            Standing standing = new Standing(table.positionOf(team), row.getPlayed(), row.getWon(), row.getDrawn(),
                    row.getLost(), row.getGoalsFor(), row.getGoalsAgainst(), row.getGoalDifference(), row.getPoints(),
                    row.getForm().chars().mapToObj(c -> String.valueOf((char) c)).toList());

            Map<Integer, SquadLine> totals = statsDao.findSquadTotals(season.getId(), id).stream()
                    .collect(Collectors.toMap(SquadLine::playerId, Function.identity()));
            List<SquadPlayer> squad = team.getSquad().stream()
                    .sorted(Comparator.comparing((Player p) -> p.getFieldRole().ordinal())
                            .thenComparing(Comparator.comparingInt(Player::getOverall).reversed()))
                    .map(p -> squadPlayer(p, totals.get(p.getId())))
                    .toList();

            List<TeamFixture> fixtures = season.getFixturesOf(team).stream()
                    .map(fixture -> teamFixture(fixture, team))
                    .toList();

            return new TeamProfile(team.getId(), team.getName(), team.getShortName(), team.getCity(), team.getStadium(),
                    team.getCoach().getFullName(), team.getCoach().getPreferredFormation().getDisplayName(),
                    standing, squad, fixtures);
        }
    }

    private static SquadPlayer squadPlayer(Player p, SquadLine season) {
        return new SquadPlayer(p.getId(), p.getFullName(), p.getShirtNumber(), p.getPosition().name(),
                p.getFieldRole().name(), p.getOverall(), p.getAge(), p.getNationality(), p.getStatus().name(),
                season == null ? 0 : season.appearances(), season == null ? 0 : season.minutes(),
                season == null ? 0 : season.goals(), season == null ? 0 : season.assists(),
                season == null ? null : season.averageRating());
    }

    private static TeamFixture teamFixture(Fixture fixture, Team team) {
        Team opponent = fixture.opponentOf(team);
        boolean home = fixture.getHomeTeam() == team;
        if (!fixture.isPlayed()) {
            return new TeamFixture(fixture.getRound(), fixture.getId(), home, opponent.getId(), opponent.getName(),
                    opponent.getShortName(), null, null, null);
        }
        int scored = fixture.goalsFor(team);
        int conceded = fixture.goalsAgainst(team);
        String result = scored > conceded ? "W" : scored == conceded ? "D" : "L";
        return new TeamFixture(fixture.getRound(), fixture.getId(), home, opponent.getId(), opponent.getName(),
                opponent.getShortName(), scored, conceded, result);
    }
}
