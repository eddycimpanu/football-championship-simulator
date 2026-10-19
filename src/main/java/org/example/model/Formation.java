package org.example.model;

import java.util.List;

public enum Formation {
    FOUR_THREE_THREE("4-3-3",
            DetailedPosition.GK,
            DetailedPosition.LB, DetailedPosition.CB, DetailedPosition.CB, DetailedPosition.RB,
            DetailedPosition.CM, DetailedPosition.CDM, DetailedPosition.CM,
            DetailedPosition.LW, DetailedPosition.ST, DetailedPosition.RW),

    FOUR_FOUR_TWO("4-4-2",
            DetailedPosition.GK,
            DetailedPosition.LB, DetailedPosition.CB, DetailedPosition.CB, DetailedPosition.RB,
            DetailedPosition.LM, DetailedPosition.CM, DetailedPosition.CM, DetailedPosition.RM,
            DetailedPosition.ST, DetailedPosition.ST),

    FOUR_TWO_THREE_ONE("4-2-3-1",
            DetailedPosition.GK,
            DetailedPosition.LB, DetailedPosition.CB, DetailedPosition.CB, DetailedPosition.RB,
            DetailedPosition.CDM, DetailedPosition.CDM,
            DetailedPosition.LW, DetailedPosition.CAM, DetailedPosition.RW,
            DetailedPosition.ST),

    THREE_FIVE_TWO("3-5-2",
            DetailedPosition.GK,
            DetailedPosition.CB, DetailedPosition.CB, DetailedPosition.CB,
            DetailedPosition.LWB, DetailedPosition.CM, DetailedPosition.CDM, DetailedPosition.CM, DetailedPosition.RWB,
            DetailedPosition.ST, DetailedPosition.ST);

    public static final int PLAYERS_ON_PITCH = 11;

    private final String displayName;
    private final List<DetailedPosition> slots;

    Formation(String displayName, DetailedPosition... slots) {
        if (slots.length != PLAYERS_ON_PITCH) {
            throw new IllegalArgumentException(displayName + " must have exactly 11 positions");
        }
        this.displayName = displayName;
        this.slots = List.of(slots);
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<DetailedPosition> getSlots() {
        return slots;
    }

    public long countPlayers(FieldRole fieldRole) {
        return slots.stream()
                .filter(position -> position.getFieldRole() == fieldRole)
                .count();
    }

    @Override
    public String toString() {
        return displayName;
    }
}