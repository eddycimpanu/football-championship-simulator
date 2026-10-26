package org.example.betting.agents;

import org.example.betting.Bettor;
import org.example.betting.BettingException;
import org.example.betting.BettingService;
import org.example.betting.BettingStore;
import org.example.betting.MatchOdds;
import org.example.config.BettingConfig;
import org.example.league.Fixture;
import org.example.league.LeagueTable;
import org.example.league.Round;
import org.example.league.Season;
import org.example.league.StandingRow;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Lets the AI agents bet on the next round, once each, through the same rules as you.
public class AgentService {

    private final BettingService bettingService;
    private final BettingStore store;
    private final List<Agent> agents;

    public AgentService(BettingService bettingService, BettingStore store, List<Agent> agents) {
        this.bettingService = bettingService;
        this.store = store;
        this.agents = List.copyOf(agents);
    }

    public List<Agent> getAgents() {
        return agents;
    }

    // Every agent's bettor in this season, created with the starting credits the first time
    public Bettor bettorOf(Season season, Agent agent) {
        return store.findOrCreateBettor(season.getId(), agent.name(), "AGENT", agent.key(),
                BettingConfig.STARTING_BALANCE);
    }

    // Called when the odds of the round are fixed. An agent that already has a ticket on the round is skipped.
    public void placeTickets(Season season, Round round, Map<Integer, MatchOdds> odds) {
        List<MatchView> matches = matchViews(season, round, odds);
        if (matches.isEmpty()) {
            return;
        }
        for (Agent agent : agents) {
            Bettor bettor = bettorOf(season, agent);
            boolean alreadyBet = store.findTickets(bettor.id()).stream().anyMatch(t -> t.round() == round.getNumber());
            if (alreadyBet) {
                continue;
            }
            for (TicketPlan plan : agent.strategy().decide(matches, bettor.balance())) {
                try {
                    bettingService.place(season, bettor, plan.picks(), plan.stake());
                    bettor = bettorOf(season, agent);   // the balance went down
                } catch (BettingException e) {
                    // A plan the rules refuse (e.g. not enough credits left) is simply skipped
                }
            }
        }
    }

    // What the agents can see: each team's form from the table and the bookmaker's odds
    private static List<MatchView> matchViews(Season season, Round round, Map<Integer, MatchOdds> odds) {
        LeagueTable table = season.getTable();
        List<MatchView> views = new ArrayList<>();
        for (Fixture fixture : round.getFixtures()) {
            MatchOdds matchOdds = odds.get(fixture.getId());
            if (matchOdds != null && !fixture.isPlayed()) {
                views.add(new MatchView(fixture.getId(), fixture.getHomeTeam().getName(),
                        fixture.getAwayTeam().getName(), form(table, fixture.getHomeTeam()),
                        form(table, fixture.getAwayTeam()), matchOdds));
            }
        }
        return views;
    }

    private static TeamForm form(LeagueTable table, Team team) {
        StandingRow row = table.rowOf(team);
        int played = row.getPlayed();
        if (played == 0) {
            return new TeamForm(0, 0, 0, 0);
        }
        return new TeamForm(played, (double) row.getPoints() / played, (double) row.getGoalsFor() / played,
                (double) row.getGoalsAgainst() / played);
    }
}
