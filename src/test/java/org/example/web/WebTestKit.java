package org.example.web;

import org.example.TestData;
import org.example.betting.BettingService;
import org.example.betting.OddsCalculator;
import org.example.betting.OddsService;
import org.example.betting.agents.Agent;
import org.example.betting.agents.AgentService;
import org.example.league.InMemorySeasonStore;
import org.example.league.LeagueSource;
import org.example.league.SeasonService;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.testing.InMemoryBettingStore;
import org.example.testing.InMemoryOddsStore;

import java.util.ArrayList;
import java.util.List;

// The web application wired like AppConfig does it, but with in-memory stores instead of PostgreSQL.
// The controllers are plain classes, so the tests call their methods directly.
final class WebTestKit {

    // Few simulations per match: the tests check the rules, not the precision of the odds
    private static final int SIMULATIONS = 40;

    final InMemoryOddsStore oddsStore = new InMemoryOddsStore();
    final SeasonService seasonService;
    final InMemoryBettingStore bettingStore;
    final BettingService bettingService;
    final AgentService agentService;

    final SeasonController seasons;
    final BettingController betting;
    final AgentController agents;
    final BettorsController bettors;
    final ResetController reset;

    WebTestKit() {
        LeagueSource league = new TestLeague();
        seasonService = SeasonService.startNewSeason("Test League", league.loadTeams(), league.loadReferees(), 21,
                new InMemorySeasonStore());
        bettingStore = new InMemoryBettingStore(seasonService::getSeason);
        OddsService oddsService = new OddsService(new OddsCalculator(SIMULATIONS, 0.05), oddsStore);
        bettingService = new BettingService(oddsService, bettingStore);
        agentService = new AgentService(bettingService, bettingStore, Agent.all());

        seasons = new SeasonController(seasonService, oddsService, bettingService, agentService);
        betting = new BettingController(seasonService, bettingService);
        agents = new AgentController(seasonService, agentService, bettingService);
        bettors = new BettorsController(seasonService, bettingService, agentService);
        reset = new ResetController(seasonService, league);
    }

    // 20 clubs from 70 to 89, so there are clear favourites and clear underdogs
    private static final class TestLeague implements LeagueSource {
        @Override
        public List<Team> loadTeams() {
            List<Team> teams = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                teams.add(TestData.team(String.format("Club %02d", i + 1), 70 + i));
            }
            return teams;
        }

        @Override
        public List<Referee> loadReferees() {
            return TestData.referees(12);
        }
    }
}
