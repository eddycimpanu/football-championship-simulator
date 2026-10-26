package org.example.betting.agents;

import org.example.betting.Pick;

import java.math.BigDecimal;
import java.util.List;

// A ticket an agent wants to place: its selections and its stake.
public record TicketPlan(List<Pick> picks, BigDecimal stake) {

    public TicketPlan {
        picks = List.copyOf(picks);
    }
}
