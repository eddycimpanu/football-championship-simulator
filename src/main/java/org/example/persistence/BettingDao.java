package org.example.persistence;

import org.example.betting.BetStatus;
import org.example.betting.Bettor;
import org.example.betting.BettingException;
import org.example.betting.BettingStore;
import org.example.betting.Selection;
import org.example.betting.Ticket;
import org.example.betting.TicketSelection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BettingDao implements BettingStore {

    private static final String SELECT_BETTOR = """
            SELECT id, season_id, name, kind, strategy, starting_balance, balance
            FROM bettors
            """;

    // Inserts the bettor only if it is not there yet, then reads it back
    private static final String INSERT_BETTOR = """
            INSERT INTO bettors (season_id, name, kind, strategy, starting_balance, balance)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (season_id, name) DO NOTHING
            """;

    // The stake comes off the balance only if the balance is big enough: one statement, no race
    private static final String TAKE_STAKE =
            "UPDATE bettors SET balance = balance - ? WHERE id = ? AND balance >= ?";

    private static final String INSERT_TICKET = """
            INSERT INTO tickets (bettor_id, round, stake, total_odds)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

    private static final String INSERT_SELECTION = """
            INSERT INTO ticket_selections (ticket_id, match_id, selection, odds)
            VALUES (?, ?, ?, ?)
            """;

    // Tickets with their selections and matches, one row per selection
    private static final String SELECT_TICKETS = """
            SELECT t.id, t.bettor_id, t.round AS ticket_round, t.stake, t.total_odds, t.status, t.payout,
                   s.id AS selection_id, s.match_id, s.selection, s.odds, s.status AS selection_status,
                   m.round, m.status AS match_status, m.home_goals, m.away_goals,
                   h.name AS home_team, h.short_name AS home_short, a.name AS away_team, a.short_name AS away_short
            FROM tickets t
            JOIN ticket_selections s ON s.ticket_id = t.id
            JOIN matches m ON m.id = s.match_id
            JOIN teams h ON h.id = m.home_team_id
            JOIN teams a ON a.id = m.away_team_id
            """;

    // Step 1 of settling: every open selection whose match is over becomes WON or LOST
    private static final String SELECTIONS_TO_SETTLE = """
            SELECT s.id, s.selection, m.home_goals, m.away_goals
            FROM ticket_selections s
            JOIN tickets t ON t.id = s.ticket_id
            JOIN bettors r ON r.id = t.bettor_id
            JOIN matches m ON m.id = s.match_id
            WHERE r.season_id = ? AND s.status = 'OPEN' AND m.status = 'PLAYED'
            FOR UPDATE OF s
            """;

    private static final String SETTLE_SELECTION = "UPDATE ticket_selections SET status = ? WHERE id = ?";

    // Step 2: every open ticket with the status of its selections (FOR UPDATE: no ticket is paid twice)
    private static final String OPEN_TICKETS = """
            SELECT t.id, t.bettor_id, t.stake, t.total_odds, s.status
            FROM tickets t
            JOIN bettors r ON r.id = t.bettor_id
            JOIN ticket_selections s ON s.ticket_id = t.id
            WHERE r.season_id = ? AND t.status = 'OPEN'
            ORDER BY t.id
            FOR UPDATE OF t
            """;

    private static final String SETTLE_TICKET =
            "UPDATE tickets SET status = ?, payout = ?, settled_at = now() WHERE id = ?";

    private static final String PAY_OUT = "UPDATE bettors SET balance = balance + ? WHERE id = ?";

    private final ConnectionProvider connectionProvider;

    public BettingDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Bettor findOrCreateBettor(int seasonId, String name, String kind, String strategy, BigDecimal startingBalance) {
        try (Connection connection = connectionProvider.getConnection()) {
            try (PreparedStatement insert = connection.prepareStatement(INSERT_BETTOR)) {
                insert.setInt(1, seasonId);
                insert.setString(2, name);
                insert.setString(3, kind);
                insert.setString(4, strategy);
                insert.setBigDecimal(5, startingBalance);
                insert.setBigDecimal(6, startingBalance);
                insert.executeUpdate();
            }
            try (PreparedStatement select = connection.prepareStatement(SELECT_BETTOR + "WHERE season_id = ? AND name = ?")) {
                select.setInt(1, seasonId);
                select.setString(2, name);
                try (ResultSet rs = select.executeQuery()) {
                    rs.next();
                    return bettor(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the bettor " + name, e);
        }
    }

    @Override
    public List<Bettor> findBettors(int seasonId) {
        List<Bettor> bettors = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BETTOR + "WHERE season_id = ? ORDER BY balance DESC, name")) {
            statement.setInt(1, seasonId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    bettors.add(bettor(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the bettors", e);
        }
        return bettors;
    }

    @Override
    public List<Ticket> findTickets(int bettorId) {
        try (Connection connection = connectionProvider.getConnection()) {
            return findTickets(connection, "WHERE t.bettor_id = ? ORDER BY t.id DESC, m.id", bettorId);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the tickets of bettor " + bettorId, e);
        }
    }

    @Override
    public Ticket placeTicket(int bettorId, int round, BigDecimal stake, BigDecimal totalOdds, List<PricedPick> picks) {
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement take = connection.prepareStatement(TAKE_STAKE)) {
                    take.setBigDecimal(1, stake);
                    take.setInt(2, bettorId);
                    take.setBigDecimal(3, stake);
                    if (take.executeUpdate() == 0) {
                        throw new BettingException("Not enough credits for a stake of " + stake);
                    }
                }
                int ticketId;
                try (PreparedStatement insert = connection.prepareStatement(INSERT_TICKET)) {
                    insert.setInt(1, bettorId);
                    insert.setInt(2, round);
                    insert.setBigDecimal(3, stake);
                    insert.setBigDecimal(4, totalOdds);
                    try (ResultSet rs = insert.executeQuery()) {
                        rs.next();
                        ticketId = rs.getInt(1);
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(INSERT_SELECTION)) {
                    for (PricedPick pick : picks) {
                        insert.setInt(1, ticketId);
                        insert.setInt(2, pick.matchId());
                        insert.setString(3, pick.selection().name());
                        insert.setBigDecimal(4, pick.odds());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
                return findTickets(connection, "WHERE t.id = ? ORDER BY m.id", ticketId).get(0);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not place the ticket", e);
        }
    }

    // Selections first, then tickets, then payments: all in one transaction
    @Override
    public List<Ticket> settlePlayedTickets(int seasonId) {
        List<Integer> settled = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                settleSelections(connection, seasonId);
                settled.addAll(settleTickets(connection, seasonId));
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
            if (settled.isEmpty()) {
                return List.of();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_TICKETS + "WHERE t.id = ANY (?) ORDER BY t.id, m.id")) {
                statement.setArray(1, connection.createArrayOf("integer", settled.toArray()));
                return readTickets(statement);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not settle the tickets", e);
        }
    }

    private void settleSelections(Connection connection, int seasonId) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(SELECTIONS_TO_SETTLE);
             PreparedStatement update = connection.prepareStatement(SETTLE_SELECTION)) {
            select.setInt(1, seasonId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    boolean won = Selection.valueOf(rs.getString("selection"))
                            .wins(rs.getInt("home_goals"), rs.getInt("away_goals"));
                    update.setString(1, won ? "WON" : "LOST");
                    update.setInt(2, rs.getInt("id"));
                    update.addBatch();
                }
            }
            update.executeBatch();
        }
    }

    private record OpenTicket(int bettorId, BigDecimal stake, BigDecimal totalOdds, List<BetStatus> selections) {
    }

    private List<Integer> settleTickets(Connection connection, int seasonId) throws SQLException {
        Map<Integer, OpenTicket> open = new LinkedHashMap<>();
        try (PreparedStatement select = connection.prepareStatement(OPEN_TICKETS)) {
            select.setInt(1, seasonId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    if (!open.containsKey(id)) {
                        open.put(id, new OpenTicket(rs.getInt("bettor_id"), rs.getBigDecimal("stake"),
                                rs.getBigDecimal("total_odds"), new ArrayList<>()));
                    }
                    open.get(id).selections().add(BetStatus.valueOf(rs.getString("status")));
                }
            }
        }

        List<Integer> settled = new ArrayList<>();
        try (PreparedStatement update = connection.prepareStatement(SETTLE_TICKET);
             PreparedStatement pay = connection.prepareStatement(PAY_OUT)) {
            for (Map.Entry<Integer, OpenTicket> entry : open.entrySet()) {
                OpenTicket ticket = entry.getValue();
                BetStatus status = Ticket.statusOf(ticket.selections());
                if (status == BetStatus.OPEN) {
                    continue;
                }
                BigDecimal payout = status == BetStatus.WON
                        ? Ticket.payoutFor(ticket.stake(), ticket.totalOdds())
                        : BigDecimal.ZERO.setScale(2);
                update.setString(1, status.name());
                update.setBigDecimal(2, payout);
                update.setInt(3, entry.getKey());
                update.addBatch();
                if (status == BetStatus.WON) {
                    pay.setBigDecimal(1, payout);
                    pay.setInt(2, ticket.bettorId());
                    pay.addBatch();
                }
                settled.add(entry.getKey());
            }
            update.executeBatch();
            pay.executeBatch();
        }
        return settled;
    }

    // ----- Reading rows -----

    private List<Ticket> findTickets(Connection connection, String where, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_TICKETS + where)) {
            statement.setInt(1, id);
            return readTickets(statement);
        }
    }

    // Groups the rows (one per selection) back into tickets, keeping their order
    private static List<Ticket> readTickets(PreparedStatement statement) throws SQLException {
        Map<Integer, Ticket> tickets = new LinkedHashMap<>();
        Map<Integer, List<TicketSelection>> selections = new LinkedHashMap<>();
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int ticketId = rs.getInt("id");
                if (!tickets.containsKey(ticketId)) {
                    tickets.put(ticketId, new Ticket(ticketId, rs.getInt("bettor_id"), rs.getInt("ticket_round"),
                            rs.getBigDecimal("stake"), rs.getBigDecimal("total_odds"),
                            BetStatus.valueOf(rs.getString("status")), rs.getBigDecimal("payout"), List.of()));
                    selections.put(ticketId, new ArrayList<>());
                }
                boolean played = "PLAYED".equals(rs.getString("match_status"));
                selections.get(ticketId).add(new TicketSelection(rs.getInt("selection_id"), rs.getInt("match_id"),
                        rs.getInt("round"), rs.getString("home_team"), rs.getString("home_short"),
                        rs.getString("away_team"), rs.getString("away_short"),
                        played ? rs.getInt("home_goals") : null, played ? rs.getInt("away_goals") : null,
                        Selection.valueOf(rs.getString("selection")), rs.getBigDecimal("odds"),
                        BetStatus.valueOf(rs.getString("selection_status"))));
            }
        }
        List<Ticket> result = new ArrayList<>();
        tickets.forEach((id, t) -> result.add(new Ticket(t.id(), t.bettorId(), t.round(), t.stake(), t.totalOdds(),
                t.status(), t.payout(), selections.get(id))));
        return result;
    }

    private static Bettor bettor(ResultSet rs) throws SQLException {
        return new Bettor(rs.getInt("id"), rs.getInt("season_id"), rs.getString("name"), rs.getString("kind"),
                rs.getString("strategy"), rs.getBigDecimal("starting_balance"), rs.getBigDecimal("balance"));
    }
}
