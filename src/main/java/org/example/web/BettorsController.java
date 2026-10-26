package org.example.web;

import org.example.betting.Bettor;
import org.example.betting.BettingService;
import org.example.betting.BettorStats;
import org.example.betting.Ticket;
import org.example.betting.agents.Agent;
import org.example.betting.agents.AgentService;
import org.example.league.Season;
import org.example.league.SeasonService;
import org.example.league.StandingRow;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// The bettors table: you against the agents, richest first, with the balance after every round.
@RestController
@RequestMapping("/api")
public class BettorsController {

    public record Champion(int teamId, String team, String teamShort, int points) {
    }

    // The champion is only sent when the season is over.
    public record BettorsResponse(String season, int roundsPlayed, int totalRounds, boolean finished,
                                  Champion champion, List<BettorStats> bettors) {
    }

    private final SeasonService seasonService;
    private final BettingService bettingService;
    private final AgentService agentService;

    public BettorsController(SeasonService seasonService, BettingService bettingService, AgentService agentService) {
        this.seasonService = seasonService;
        this.bettingService = bettingService;
        this.agentService = agentService;
    }

    @GetMapping("/bettors")
    public BettorsResponse bettors() {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            bettingService.settle(season);
            int roundsPlayed = season.getRoundsPlayed();

            List<BettorStats> stats = new ArrayList<>();
            Bettor you = bettingService.human(season);
            stats.add(BettorStats.of(you, "That's you. Pick your odds before every round.",
                    bettingService.ticketsOf(you), roundsPlayed));
            for (Agent agent : agentService.getAgents()) {
                Bettor bettor = agentService.bettorOf(season, agent);
                stats.add(BettorStats.of(bettor, agent.strategy().description(), bettingService.ticketsOf(bettor),
                        roundsPlayed));
            }
            stats.sort(Comparator.comparing((BettorStats s) -> s.bettor().balance()).reversed());

            Champion champion = null;
            if (season.isFinished()) {
                StandingRow leader = season.getTable().getLeader();
                champion = new Champion(leader.getTeam().getId(), leader.getTeam().getName(),
                        leader.getTeam().getShortName(), leader.getPoints());
            }
            return new BettorsResponse(season.getName(), roundsPlayed, season.getNumberOfRounds(),
                    season.isFinished(), champion, stats);
        }
    }

    // Every ticket of one bettor of the current season, newest first.
    @GetMapping("/bettors/{id}/tickets")
    public List<Ticket> tickets(@PathVariable int id) {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            List<Bettor> bettors = new ArrayList<>(List.of(bettingService.human(season)));
            agentService.getAgents().forEach(agent -> bettors.add(agentService.bettorOf(season, agent)));
            Bettor bettor = bettors.stream().filter(b -> b.id() == id).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No bettor " + id + " in this season"));
            return bettingService.ticketsOf(bettor);
        }
    }
}
