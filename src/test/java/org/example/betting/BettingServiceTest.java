package org.example.betting;

import org.example.TestData;
import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BettingServiceTest {

    // ----- Fakes: the service only knows the two interfaces, so the tests need no database -----

    private static final class FakeOddsStore implements OddsStore {
        final Map<Integer, MatchOdds> odds = new HashMap<>();

        @Override
        public Map<Integer, MatchOdds> findByMatchIds(Collection<Integer> ids) {
            Map<Integer, MatchOdds> found = new HashMap<>();
            ids.forEach(id -> {
                if (odds.containsKey(id)) found.put(id, odds.get(id));
            });
            return found;
        }

        @Override
        public void saveAll(List<MatchOdds> list) {
            list.forEach(o -> odds.put(o.matchId(), o));
        }
    }

    private static final class FakeBettingStore implements BettingStore {
        Bettor bettor;
        final List<Ticket> tickets = new ArrayList<>();

        @Override
        public Bettor findOrCreateBettor(int seasonId, String name, String kind, String strategy, BigDecimal start) {
            if (bettor == null) bettor = new Bettor(1, seasonId, name, kind, strategy, start, start);
            return bettor;
        }

        @Override
        public List<Bettor> findBettors(int seasonId) {
            return List.of(bettor);
        }

        @Override
        public List<Ticket> findTickets(int bettorId) {
            return tickets;
        }

        @Override
        public Ticket placeTicket(int bettorId, int round, BigDecimal stake, BigDecimal totalOdds, List<PricedPick> picks) {
            if (bettor.balance().compareTo(stake) < 0) throw new BettingException("Not enough credits");
            bettor = new Bettor(1, bettor.seasonId(), bettor.name(), bettor.kind(), null, bettor.startingBalance(),
                    bettor.balance().subtract(stake));
            List<TicketSelection> selections = picks.stream().map(p -> new TicketSelection(0, p.matchId(), round,
                    "Home", "HOM", "Away", "AWA", null, null, p.selection(), p.odds(), BetStatus.OPEN)).toList();
            Ticket ticket = new Ticket(tickets.size() + 1, bettorId, round, stake, totalOdds, BetStatus.OPEN, null, selections);
            tickets.add(ticket);
            return ticket;
        }

        @Override
        public List<Ticket> settlePlayedTickets(int seasonId) {
            return List.of();
        }
    }

    private Season season;
    private FakeBettingStore bettingStore;
    private BettingService service;
    private Bettor you;
    private List<Fixture> roundOne;

    @BeforeEach
    void setUp() {
        season = Season.create("Test", TestData.league(20, 78), TestData.referees(12), 4);
        int id = 1;
        for (Round round : season.getRounds()) {
            for (Fixture fixture : round.getFixtures()) {
                fixture.assignId(id++);
            }
        }
        FakeOddsStore oddsStore = new FakeOddsStore();
        // Ready-made odds for round 1, so no simulation runs in these tests: 1/X/2 at 3.00, the rest at 2.00
        Map<Selection, Double> chances = new EnumMap<>(Selection.class);
        for (Selection selection : Selection.values()) {
            chances.put(selection, selection.getMarket() == Selection.Market.RESULT ? 1 / 3.0 : 0.5);
        }
        roundOne = season.getRound(1).getFixtures();
        for (Fixture fixture : roundOne) {
            oddsStore.odds.put(fixture.getId(), MatchOdds.fromProbabilities(fixture.getId(), 100, chances, 0.0));
        }
        bettingStore = new FakeBettingStore();
        service = new BettingService(new OddsService(new OddsCalculator(1, 0.05), oddsStore), bettingStore);
        you = service.human(season);
    }

    private Pick pick(int match, Selection selection) {
        return new Pick(roundOne.get(match).getId(), selection);
    }

    @Test
    void everyoneStartsWithOneThousandCredits() {
        assertEquals(new BigDecimal("1000.00"), you.balance());
    }

    @Test
    void theOddsOfATicketAreMultiplied() {
        Ticket ticket = service.place(season, you,
                List.of(pick(0, Selection.HOME), pick(1, Selection.OVER_2_5), pick(2, Selection.BOTH_SCORE)),
                new BigDecimal("10"));

        assertEquals(new BigDecimal("12.00"), ticket.totalOdds());          // 3.00 x 2.00 x 2.00
        assertEquals(new BigDecimal("120.00"), ticket.potentialReturn());
        assertEquals(new BigDecimal("990.00"), bettingStore.bettor.balance());
    }

    @Test
    void aTicketCanHaveOnlyOneSelectionPerMatch() {
        assertThrows(BettingException.class, () -> service.place(season, you,
                List.of(pick(0, Selection.BOTH_SCORE), pick(0, Selection.NOT_BOTH_SCORE)), new BigDecimal("10")));
    }

    @Test
    void anEmptyTicketIsRefused() {
        assertThrows(BettingException.class, () -> service.place(season, you, List.of(), new BigDecimal("10")));
    }

    @Test
    void aStakeBiggerThanTheBalanceIsRefused() {
        assertThrows(BettingException.class,
                () -> service.place(season, you, List.of(pick(0, Selection.HOME)), new BigDecimal("1000.01")));
    }

    @Test
    void stakesBelowOneCreditOrWithThreeDecimalsAreRefused() {
        assertThrows(BettingException.class,
                () -> service.place(season, you, List.of(pick(0, Selection.DRAW)), new BigDecimal("0.50")));
        assertThrows(BettingException.class,
                () -> service.place(season, you, List.of(pick(0, Selection.DRAW)), new BigDecimal("2.555")));
    }

    @Test
    void onlyMatchesOfTheNextRoundCanBeBetOn() {
        Pick later = new Pick(season.getRound(5).getFixtures().get(0).getId(), Selection.AWAY);
        assertThrows(BettingException.class,
                () -> service.place(season, you, List.of(pick(0, Selection.HOME), later), new BigDecimal("10")));
    }

    @Test
    void oneLostSelectionLosesTheWholeTicket() {
        assertEquals(BetStatus.LOST, Ticket.statusOf(List.of(BetStatus.WON, BetStatus.WON, BetStatus.LOST)));
        assertEquals(BetStatus.LOST, Ticket.statusOf(List.of(BetStatus.LOST, BetStatus.OPEN)));
        assertEquals(BetStatus.OPEN, Ticket.statusOf(List.of(BetStatus.WON, BetStatus.OPEN)));
        assertEquals(BetStatus.WON, Ticket.statusOf(List.of(BetStatus.WON, BetStatus.WON)));
    }

    @Test
    void paymentsAreRoundedToTheCent() {
        // 33.33 x 1.87 = 62.3271 → 62.33
        assertEquals(new BigDecimal("62.33"), Ticket.payoutFor(new BigDecimal("33.33"), new BigDecimal("1.87")));
        // 1.35 x 1.47 x 2.13 = 4.2269... → 4.23
        assertEquals(new BigDecimal("4.23"), Ticket.totalOdds(
                List.of(new BigDecimal("1.35"), new BigDecimal("1.47"), new BigDecimal("2.13"))));
    }
}
