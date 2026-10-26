package org.example.web;

import org.example.betting.BetStatus;
import org.example.betting.Bettor;
import org.example.betting.BettingException;
import org.example.betting.BettingService;
import org.example.betting.Pick;
import org.example.betting.Selection;
import org.example.betting.Ticket;
import org.example.league.Season;
import org.example.league.SeasonService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

// Your tickets: GET /api/betting/me and POST /api/tickets
@RestController
@RequestMapping("/api")
public class BettingController {

    public record PickRequest(int matchId, String selection) {
    }

    public record PlaceTicketRequest(BigDecimal stake, List<PickRequest> selections) {
    }

    public record PlaceTicketResponse(Ticket ticket, Bettor bettor) {
    }

    public record Summary(int won, int lost, int open, BigDecimal staked, BigDecimal returned, BigDecimal profit) {
    }

    public record MyBettingResponse(Bettor bettor, Summary summary, List<Ticket> open, List<Ticket> settled) {
    }

    private final SeasonService seasonService;
    private final BettingService bettingService;

    public BettingController(SeasonService seasonService, BettingService bettingService) {
        this.seasonService = seasonService;
        this.bettingService = bettingService;
    }

    @GetMapping("/betting/me")
    public MyBettingResponse me() {
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            bettingService.settle(season);   // pays tickets of rounds played from the console too
            Bettor you = bettingService.human(season);
            List<Ticket> tickets = bettingService.ticketsOf(you);
            List<Ticket> open = tickets.stream().filter(t -> t.status() == BetStatus.OPEN).toList();
            List<Ticket> settled = tickets.stream().filter(t -> t.status() != BetStatus.OPEN).toList();
            return new MyBettingResponse(you, summary(you, tickets), open, settled);
        }
    }

    @PostMapping("/tickets")
    public PlaceTicketResponse place(@RequestBody PlaceTicketRequest request) {
        List<Pick> picks = toPicks(request.selections());
        synchronized (seasonService) {
            Season season = seasonService.getSeason();
            try {
                Bettor you = bettingService.human(season);
                Ticket ticket = bettingService.place(season, you, picks, request.stake());
                return new PlaceTicketResponse(ticket, bettingService.human(season));
            } catch (BettingException e) {
                // A broken rule is the user's mistake: 400 with a message the page can show
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
    }

    private static List<Pick> toPicks(List<PickRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(r -> {
            try {
                return new Pick(r.matchId(), Selection.valueOf(r.selection()));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown selection " + r.selection());
            }
        }).toList();
    }

    private static Summary summary(Bettor you, List<Ticket> tickets) {
        int won = (int) tickets.stream().filter(t -> t.status() == BetStatus.WON).count();
        int lost = (int) tickets.stream().filter(t -> t.status() == BetStatus.LOST).count();
        int open = tickets.size() - won - lost;
        BigDecimal staked = tickets.stream().filter(t -> t.status() != BetStatus.OPEN)
                .map(Ticket::stake).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returned = tickets.stream().filter(t -> t.status() == BetStatus.WON)
                .map(Ticket::payout).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Summary(won, lost, open, staked, returned, you.profit());
    }
}
