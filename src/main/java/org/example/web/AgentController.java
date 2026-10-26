package org.example.web;

import org.example.betting.Bettor;
import org.example.betting.BettingService;
import org.example.betting.Ticket;
import org.example.betting.agents.Agent;
import org.example.betting.agents.AgentService;
import org.example.league.Season;
import org.example.league.SeasonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

// The AI agents: GET /api/agents and GET /api/rounds/{number}/agent-tickets
@RestController
@RequestMapping("/api")
public class AgentController {

    public record AgentSummary(String key, String name, String description, Bettor bettor) {
    }

    public record AgentTicket(String key, String name, Ticket ticket) {
    }

    private final SeasonService seasonService;
    private final AgentService agentService;
    private final BettingService bettingService;

    public AgentController(SeasonService seasonService, AgentService agentService, BettingService bettingService) {
        this.seasonService = seasonService;
        this.agentService = agentService;
        this.bettingService = bettingService;
    }

    @GetMapping("/agents")
    public List<AgentSummary> agents() {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            bettingService.settle(season);
            return agentService.getAgents().stream()
                    .map(a -> new AgentSummary(a.key(), a.name(), a.strategy().description(), agentService.bettorOf(season, a)))
                    .toList();
        }
    }

    // What each agent bet on a round (nothing yet if the round's odds have not been fixed)
    @GetMapping("/rounds/{number}/agent-tickets")
    public List<AgentTicket> agentTickets(@PathVariable int number) {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            bettingService.settle(season);
            List<AgentTicket> tickets = new ArrayList<>();
            for (Agent agent : agentService.getAgents()) {
                Bettor bettor = agentService.bettorOf(season, agent);
                bettingService.ticketsOf(bettor).stream()
                        .filter(t -> t.round() == number)
                        .forEach(t -> tickets.add(new AgentTicket(agent.key(), agent.name(), t)));
            }
            return tickets;
        }
    }
}
