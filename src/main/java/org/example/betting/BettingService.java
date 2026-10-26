package org.example.betting;

import org.example.config.BettingConfig;
import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The betting rules: what can go on a ticket, with how much, and paying out after the round.
public class BettingService {

    private final OddsService oddsService;
    private final BettingStore store;

    public BettingService(OddsService oddsService, BettingStore store) {
        this.oddsService = oddsService;
        this.store = store;
    }

    // You, in this season (with the starting credits the first time)
    public Bettor human(Season season) {
        return store.findOrCreateBettor(season.getId(), BettingConfig.HUMAN_BETTOR, "HUMAN", null,
                BettingConfig.STARTING_BALANCE);
    }

    public List<Ticket> ticketsOf(Bettor bettor) {
        return store.findTickets(bettor.id());
    }

    // One ticket: matches of the next round only, one selection per match, priced with the saved odds
    public Ticket place(Season season, Bettor bettor, List<Pick> picks, BigDecimal stake) {
        if (season.isFinished()) {
            throw new BettingException("The season is finished");
        }
        if (picks == null || picks.isEmpty()) {
            throw new BettingException("Add at least one selection to the ticket");
        }
        checkStake(stake);

        Round next = season.getRound(season.getCurrentRoundNumber());
        Set<Integer> matches = new HashSet<>();
        for (Pick pick : picks) {
            Fixture fixture = next.getFixtures().stream()
                    .filter(f -> f.getId() == pick.matchId())
                    .findFirst()
                    .orElseThrow(() -> new BettingException("You can only bet on matches of round " + next.getNumber()));
            if (fixture.isPlayed()) {
                throw new BettingException("This match has already been played");
            }
            if (!matches.add(pick.matchId())) {
                throw new BettingException("A ticket can have only one selection per match");
            }
        }

        // The odds come from the database, never from the browser
        Map<Integer, MatchOdds> odds = oddsService.oddsFor(season, next);
        List<BettingStore.PricedPick> priced = picks.stream()
                .map(pick -> {
                    MatchOdds matchOdds = odds.get(pick.matchId());
                    if (matchOdds == null) {
                        throw new BettingException("The odds of this match are not ready yet");
                    }
                    return new BettingStore.PricedPick(pick.matchId(), pick.selection(),
                            BigDecimal.valueOf(matchOdds.oddsFor(pick.selection())));
                })
                .toList();
        BigDecimal totalOdds = Ticket.totalOdds(priced.stream().map(BettingStore.PricedPick::odds).toList());
        return store.placeTicket(bettor.id(), next.getNumber(), stake.setScale(2), totalOdds, priced);
    }

    // Pays out the tickets of every match played so far (also rounds played from the console)
    public List<Ticket> settle(Season season) {
        return store.settlePlayedTickets(season.getId());
    }

    private static void checkStake(BigDecimal stake) {
        if (stake == null) {
            throw new BettingException("Choose a stake");
        }
        if (stake.stripTrailingZeros().scale() > 2) {
            throw new BettingException("The stake can have at most two decimals");
        }
        if (stake.compareTo(BettingConfig.MIN_STAKE) < 0) {
            throw new BettingException("The smallest stake is " + BettingConfig.MIN_STAKE + " credits");
        }
    }
}
