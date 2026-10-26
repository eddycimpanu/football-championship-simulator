package org.example.web;

import org.example.betting.BetStatus;
import org.example.betting.MatchOdds;
import org.example.betting.Selection;
import org.example.betting.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BettingControllerTest {

    private WebTestKit kit;
    private List<MatchOdds> roundOne;

    @BeforeEach
    void setUp() {
        kit = new WebTestKit();
        roundOne = kit.seasons.odds(1).matches();
    }

    private BettingController.PickRequest pick(int match, Selection selection) {
        return new BettingController.PickRequest(roundOne.get(match).matchId(), selection.name());
    }

    private BettingController.PlaceTicketResponse place(String stake, BettingController.PickRequest... picks) {
        return kit.betting.place(new BettingController.PlaceTicketRequest(new BigDecimal(stake), List.of(picks)));
    }

    @Test
    void youStartWithOneThousandCreditsAndNoTickets() {
        BettingController.MyBettingResponse me = kit.betting.me();

        assertEquals("You", me.bettor().name());
        assertEquals(0, new BigDecimal("1000").compareTo(me.bettor().balance()));
        assertTrue(me.open().isEmpty());
        assertTrue(me.settled().isEmpty());
    }

    @Test
    void aTicketTakesTheStakeAndUsesTheServerOdds() {
        BettingController.PlaceTicketResponse response = place("25", pick(0, Selection.HOME), pick(1, Selection.OVER_2_5));

        double expected = roundOne.get(0).oddsFor(Selection.HOME) * roundOne.get(1).oddsFor(Selection.OVER_2_5);
        assertEquals(expected, response.ticket().totalOdds().doubleValue(), 0.01);
        assertEquals(new BigDecimal("975.00"), response.bettor().balance());
        assertEquals(1, kit.betting.me().summary().open());
    }

    @Test
    void afterTheRoundTheTicketIsPaidOrLost() {
        Ticket ticket = place("40", pick(0, Selection.HOME), pick(1, Selection.BOTH_SCORE)).ticket();

        kit.seasons.playNextRound();

        BettingController.MyBettingResponse me = kit.betting.me();
        Ticket settled = me.settled().get(0);
        assertEquals(ticket.id(), settled.id());
        assertNotEquals(BetStatus.OPEN, settled.status());
        settled.selections().forEach(s -> assertNotEquals(BetStatus.OPEN, s.status()));

        // The balance is exactly 1000 - stake (+ payout if every selection won)
        BigDecimal expected = new BigDecimal("960.00");
        if (settled.status() == BetStatus.WON) {
            expected = expected.add(Ticket.payoutFor(settled.stake(), settled.totalOdds()));
        }
        assertEquals(expected, me.bettor().balance());
        assertEquals(me.bettor().profit(), me.summary().profit());
    }

    @Test
    void brokenRulesAnswerBadRequestWithTheReason() {
        ResponseStatusException twoOnOneMatch = assertThrows(ResponseStatusException.class,
                () -> place("10", pick(0, Selection.BOTH_SCORE), pick(0, Selection.NOT_BOTH_SCORE)));
        assertEquals(HttpStatus.BAD_REQUEST, twoOnOneMatch.getStatusCode());

        ResponseStatusException tooMuch = assertThrows(ResponseStatusException.class,
                () -> place("1000.01", pick(0, Selection.HOME)));
        assertEquals(HttpStatus.BAD_REQUEST, tooMuch.getStatusCode());
        assertTrue(tooMuch.getReason().length() > 5);    // a message the page can show
    }

    @Test
    void anUnknownSelectionIsRefused() {
        BettingController.PickRequest nonsense = new BettingController.PickRequest(roundOne.get(0).matchId(), "WIN_BIG");
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> place("10", nonsense));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void aPlayedRoundCanNoLongerBeBetOn() {
        kit.seasons.playNextRound();

        assertThrows(ResponseStatusException.class, () -> place("10", pick(0, Selection.HOME)));
        assertEquals(new BigDecimal("1000.00"), kit.betting.me().bettor().balance());
    }
}
