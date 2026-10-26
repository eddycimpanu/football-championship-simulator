package org.example.testing;

import org.example.betting.BetStatus;
import org.example.betting.Bettor;
import org.example.betting.BettingException;
import org.example.betting.BettingStore;
import org.example.betting.Ticket;
import org.example.betting.TicketSelection;
import org.example.engine.Score;
import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

// The betting tables (bettors, tickets, ticket_selections) kept in memory.
// It follows the same rules as BettingDao: the stake leaves the balance when the ticket is placed,
// a ticket is lost as soon as one selection loses, and the payout is added when every selection won.
// To settle, it reads the results from the season it is given.
public class InMemoryBettingStore implements BettingStore {

    private final Supplier<Season> season;
    private final Map<Integer, Bettor> bettors = new LinkedHashMap<>();
    private final Map<Integer, List<Ticket>> tickets = new LinkedHashMap<>();
    private int nextBettorId = 1;
    private int nextTicketId = 1;
    private int nextSelectionId = 1;

    public InMemoryBettingStore(Supplier<Season> season) {
        this.season = season;
    }

    @Override
    public synchronized Bettor findOrCreateBettor(int seasonId, String name, String kind, String strategy,
                                                  BigDecimal startingBalance) {
        return bettors.values().stream()
                .filter(b -> b.seasonId() == seasonId && b.name().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Bettor bettor = new Bettor(nextBettorId++, seasonId, name, kind, strategy, startingBalance,
                            startingBalance);
                    bettors.put(bettor.id(), bettor);
                    tickets.put(bettor.id(), new ArrayList<>());
                    return bettor;
                });
    }

    @Override
    public synchronized List<Bettor> findBettors(int seasonId) {
        return bettors.values().stream()
                .filter(b -> b.seasonId() == seasonId)
                .sorted(Comparator.comparing(Bettor::balance).reversed())
                .toList();
    }

    @Override
    public synchronized List<Ticket> findTickets(int bettorId) {
        List<Ticket> list = new ArrayList<>(tickets.getOrDefault(bettorId, List.of()));
        list.sort(Comparator.comparingInt(Ticket::id).reversed());
        return list;
    }

    @Override
    public synchronized Ticket placeTicket(int bettorId, int round, BigDecimal stake, BigDecimal totalOdds,
                                           List<PricedPick> picks) {
        Bettor bettor = bettors.get(bettorId);
        if (bettor.balance().compareTo(stake) < 0) {
            throw new BettingException("Not enough credits for a stake of " + stake);
        }
        bettors.put(bettorId, withBalance(bettor, bettor.balance().subtract(stake)));

        List<TicketSelection> selections = new ArrayList<>();
        for (PricedPick pick : picks) {
            Fixture fixture = fixture(pick.matchId()).orElseThrow();
            selections.add(new TicketSelection(nextSelectionId++, pick.matchId(), round,
                    fixture.getHomeTeam().getName(), fixture.getHomeTeam().getShortName(),
                    fixture.getAwayTeam().getName(), fixture.getAwayTeam().getShortName(),
                    null, null, pick.selection(), pick.odds(), BetStatus.OPEN));
        }
        Ticket ticket = new Ticket(nextTicketId++, bettorId, round, stake, totalOdds, BetStatus.OPEN, null, selections);
        tickets.get(bettorId).add(ticket);
        return ticket;
    }

    @Override
    public synchronized List<Ticket> settlePlayedTickets(int seasonId) {
        List<Ticket> settled = new ArrayList<>();
        for (Bettor bettor : findBettors(seasonId)) {
            List<Ticket> list = tickets.get(bettor.id());
            for (int i = 0; i < list.size(); i++) {
                Ticket ticket = list.get(i);
                if (ticket.status() != BetStatus.OPEN) {
                    continue;
                }
                List<TicketSelection> selections = ticket.selections().stream().map(this::settle).toList();
                BetStatus status = Ticket.statusOf(selections.stream().map(TicketSelection::status).toList());
                BigDecimal payout = switch (status) {
                    case WON -> Ticket.payoutFor(ticket.stake(), ticket.totalOdds());
                    case LOST -> BigDecimal.ZERO.setScale(2);
                    case OPEN -> null;
                };
                Ticket updated = new Ticket(ticket.id(), ticket.bettorId(), ticket.round(), ticket.stake(),
                        ticket.totalOdds(), status, payout, selections);
                list.set(i, updated);
                if (status == BetStatus.WON) {
                    Bettor current = bettors.get(bettor.id());
                    bettors.put(bettor.id(), withBalance(current, current.balance().add(payout)));
                }
                if (status != BetStatus.OPEN) {
                    settled.add(updated);
                }
            }
        }
        return settled;
    }

    private TicketSelection settle(TicketSelection s) {
        Optional<Fixture> fixture = fixture(s.matchId()).filter(Fixture::isPlayed);
        if (s.status() != BetStatus.OPEN || fixture.isEmpty()) {
            return s;
        }
        Score score = fixture.get().getScore();
        BetStatus status = s.selection().wins(score.home(), score.away()) ? BetStatus.WON : BetStatus.LOST;
        return new TicketSelection(s.id(), s.matchId(), s.round(), s.homeTeam(), s.homeShort(), s.awayTeam(),
                s.awayShort(), score.home(), score.away(), s.selection(), s.odds(), status);
    }

    private Optional<Fixture> fixture(int matchId) {
        for (Round round : season.get().getRounds()) {
            for (Fixture fixture : round.getFixtures()) {
                if (fixture.getId() == matchId) {
                    return Optional.of(fixture);
                }
            }
        }
        return Optional.empty();
    }

    private static Bettor withBalance(Bettor b, BigDecimal balance) {
        return new Bettor(b.id(), b.seasonId(), b.name(), b.kind(), b.strategy(), b.startingBalance(), balance);
    }
}
