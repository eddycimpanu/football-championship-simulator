package org.example.web;

import org.example.league.Season;
import org.example.league.SeasonService;
import org.example.model.Player;
import org.example.model.PlayerAttributes;
import org.example.model.Team;
import org.example.persistence.PlayerStatsDao;
import org.example.persistence.PlayerStatsDao.MatchLine;
import org.example.persistence.PlayerStatsDao.SeasonTotals;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// GET /api/players?search=messi and GET /api/players/{id}
@RestController
@RequestMapping("/api")
public class PlayerController {

    private static final int MAX_RESULTS = 20;
    private static final int MIN_SEARCH_LENGTH = 2;

    public record PlayerSummary(int id, String name, int teamId, String team, String teamShort, String position,
                                int overall, String nationality) {
    }

    public record PlayerProfile(int id, String name, int teamId, String team, String teamShort, String position,
                                String fieldRole, String nationality, int age, int shirtNumber, String preferredFoot,
                                int overall, PlayerAttributes attributes, String status, int condition,
                                int yellowCardsTowardsBan, SeasonTotals season, List<MatchLine> matches) {
    }

    private record SquadMember(Team team, Player player) {
    }

    private final SeasonService seasonService;
    private final PlayerStatsDao statsDao;

    public PlayerController(SeasonService seasonService, PlayerStatsDao statsDao) {
        this.seasonService = seasonService;
        this.statsDao = statsDao;
    }

    // Part of the name is enough, accents don't matter: "lacatus" finds Lăcătuș
    @GetMapping("/players")
    public List<PlayerSummary> search(@RequestParam(defaultValue = "") String search) {
        String wanted = normalize(search.trim());
        if (wanted.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }
        synchronized (seasonService) {
            return seasonService.getSeason().getTeams().stream()
                    .flatMap(team -> team.getSquad().stream().map(player -> new SquadMember(team, player)))
                    .filter(member -> normalize(member.player().getFullName()).contains(wanted))
                    .map(member -> summary(member.team(), member.player()))
                    .sorted(Comparator.comparingInt(PlayerSummary::overall).reversed())
                    .limit(MAX_RESULTS)
                    .toList();
        }
    }

    @GetMapping("/players/{id}")
    public PlayerProfile player(@PathVariable int id) {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            for (Team team : season.getTeams()) {
                for (Player p : team.getSquad()) {
                    if (p.getId() == id) {
                        return new PlayerProfile(p.getId(), p.getFullName(), team.getId(), team.getName(),
                                team.getShortName(), p.getPosition().name(), p.getFieldRole().name(),
                                p.getNationality(), p.getAge(), p.getShirtNumber(), p.getPreferredFoot().name(),
                                p.getOverall(), p.getAttributes(), p.getStatus().name(), p.getCondition(),
                                p.getYellowCardsAccumulated(),
                                statsDao.findSeasonTotals(season.getId(), id), statsDao.findMatchLines(season.getId(), id));
                    }
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no player with id " + id);
    }

    private static PlayerSummary summary(Team team, Player player) {
        return new PlayerSummary(player.getId(), player.getFullName(), team.getId(), team.getName(),
                team.getShortName(), player.getPosition().name(), player.getOverall(), player.getNationality());
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
