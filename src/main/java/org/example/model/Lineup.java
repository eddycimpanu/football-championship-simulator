package org.example.model;

import java.util.List;

public record Lineup(Formation formation, List<Player> startingEleven, List<Player> bench) {

    public Lineup {
        if (startingEleven.size() != Formation.PLAYERS_ON_PITCH) {
            throw new IllegalArgumentException("A starting eleven needs " + Formation.PLAYERS_ON_PITCH
                    + " players, got " + startingEleven.size());
        }
        startingEleven = List.copyOf(startingEleven);
        bench = List.copyOf(bench);
    }
}
