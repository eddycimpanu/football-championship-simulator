package org.example.web;

import org.example.betting.BettingService;
import org.example.betting.MatchOdds;
import org.example.betting.agents.AgentService;
import org.example.betting.OddsService;
import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;
import org.example.league.SeasonService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

// The season and its rounds: GET /api/season, GET /api/rounds/{number}, POST /api/rounds/next.
@RestController
@RequestMapping("/api")
public class SeasonController {

    public record SeasonResponse(int id, String name, int roundsPlayed, Integer nextRound, int totalRounds,
                                 boolean finished) {
    }

    // Goals are null until the match is played
    public record MatchSummary(int id, String homeTeam, String homeShort, String awayTeam, String awayShort,
                               Integer homeGoals, Integer awayGoals, String referee) {
    }

    public record RoundResponse(int number, int totalRounds, boolean played, boolean next,
                                List<MatchSummary> matches) {
    }

    // open = this is the next round, so its odds are final and betting on it is possible
    public record RoundOddsResponse(int round, boolean open, List<MatchOdds> matches) {
    }

    private final SeasonService seasonService;
    private final OddsService oddsService;
    private final BettingService bettingService;
    private final AgentService agentService;

    public SeasonController(SeasonService seasonService, OddsService oddsService, BettingService bettingService,
                            AgentService agentService) {
        this.seasonService = seasonService;
        this.oddsService = oddsService;
        this.bettingService = bettingService;
        this.agentService = agentService;
    }

    // The odds of the next round, and the agents' tickets on it (each agent bets once, when the odds are fixed)
    private Map<Integer, MatchOdds> oddsWithAgents(Season season, Round round) {
        Map<Integer, MatchOdds> odds = oddsService.oddsFor(season, round);
        if (oddsService.isNextRound(season, round)) {
            agentService.placeTickets(season, round, odds);
        }
        return odds;
    }

    @GetMapping("/season")
    public SeasonResponse season() {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            Integer next = season.isFinished() ? null : season.getCurrentRoundNumber();
            return new SeasonResponse(season.getId(), season.getName(), season.getRoundsPlayed(), next,
                    season.getNumberOfRounds(), season.isFinished());
        }
    }

    @GetMapping("/rounds/{number}")
    public RoundResponse round(@PathVariable int number) {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            if (number < 1 || number > season.getNumberOfRounds()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Round " + number + " does not exist; the season has " + season.getNumberOfRounds() + " rounds");
            }
            return toResponse(season, season.getRound(number));
        }
    }

    // The odds of a round. For the next round they are calculated the first time they are asked for
    // (1,000 simulations per match, a few seconds); played rounds show the odds they had before kick-off.
    @GetMapping("/rounds/{number}/odds")
    public RoundOddsResponse odds(@PathVariable int number) {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            if (number < 1 || number > season.getNumberOfRounds()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Round " + number + " does not exist");
            }
            Round round = season.getRound(number);
            Map<Integer, MatchOdds> byMatch = oddsWithAgents(season, round);
            // In the order of the round's matches; a match without odds is left out
            List<MatchOdds> odds = round.getFixtures().stream()
                    .map(fixture -> byMatch.get(fixture.getId()))
                    .filter(Objects::nonNull)
                    .toList();
            return new RoundOddsResponse(number, oddsService.isNextRound(season, round), odds);
        }
    }

    // Plays the next round and returns its results. One simulation at a time: a second click waits.
    // The odds are fixed first, so every round played from the web has its pre-match odds.
    @PostMapping("/rounds/next")
    public RoundResponse playNextRound() {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            if (season.isFinished()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The season is finished");
            }
            oddsWithAgents(season, season.getRound(season.getCurrentRoundNumber()));
            Round round = seasonService.playNextRound();
            bettingService.settle(season);   // the bets on this round are paid right away
            return toResponse(season, round);
        }
    }

    private static RoundResponse toResponse(Season season, Round round) {
        List<MatchSummary> matches = round.getFixtures().stream().map(SeasonController::toSummary).toList();
        boolean next = !season.isFinished() && season.getCurrentRoundNumber() == round.getNumber();
        return new RoundResponse(round.getNumber(), season.getNumberOfRounds(), round.isPlayed(), next, matches);
    }

    private static MatchSummary toSummary(Fixture fixture) {
        Integer homeGoals = fixture.isPlayed() ? fixture.getScore().home() : null;
        Integer awayGoals = fixture.isPlayed() ? fixture.getScore().away() : null;
        String referee = fixture.getReferee() == null ? null : fixture.getReferee().getFullName();
        return new MatchSummary(fixture.getId(), fixture.getHomeTeam().getName(), fixture.getHomeTeam().getShortName(),
                fixture.getAwayTeam().getName(), fixture.getAwayTeam().getShortName(), homeGoals, awayGoals, referee);
    }
}
