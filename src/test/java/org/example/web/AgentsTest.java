package org.example.web;

import org.example.betting.BetStatus;
import org.example.betting.Bettor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentsTest {

    private WebTestKit kit;

    @BeforeEach
    void setUp() {
        kit = new WebTestKit();
    }

    private Map<String, Long> ticketsPerAgent(int round) {
        return kit.agents.agentTickets(round).stream()
                .collect(Collectors.groupingBy(AgentController.AgentTicket::key, Collectors.counting()));
    }

    @Test
    void thereAreFiveAgentsWithTheirOwnMoney() {
        List<AgentController.AgentSummary> agents = kit.agents.agents();

        assertEquals(5, agents.size());
        agents.forEach(a -> assertEquals(0, new BigDecimal("1000").compareTo(a.bettor().balance())));
        assertEquals(5, agents.stream().map(a -> a.bettor().id()).distinct().count());
    }

    @Test
    void theAgentsBetWhenTheOddsAreFixedAndOnlyOnce() {
        kit.seasons.odds(1);
        Map<String, Long> first = ticketsPerAgent(1);
        assertFalse(first.isEmpty());

        kit.seasons.odds(1);                  // opening the page again
        kit.seasons.playNextRound();          // playing the round fixes the odds once more

        assertEquals(first, ticketsPerAgent(1));
    }

    @Test
    void theAgentsTicketsAreSettledAfterTheRound() {
        kit.seasons.playNextRound();

        List<AgentController.AgentTicket> tickets = kit.agents.agentTickets(1);
        assertFalse(tickets.isEmpty());
        tickets.forEach(t -> assertTrue(t.ticket().status() != BetStatus.OPEN));
    }

    @Test
    void theAgentsNeverSpendMoreThanTheyHave() {
        for (int i = 0; i < 12; i++) {
            kit.seasons.playNextRound();
        }
        Map<String, Bettor> bettors = kit.agents.agents().stream()
                .collect(Collectors.toMap(AgentController.AgentSummary::key, AgentController.AgentSummary::bettor));
        bettors.values().forEach(b -> assertTrue(b.balance().signum() >= 0, b.name()));
        // The Steady Hand bets a fixed 20 credits every round
        assertEquals(12, kit.bettingService.ticketsOf(bettors.get("STEADY")).size());
    }
}
