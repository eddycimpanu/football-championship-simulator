package org.example.league;

import java.util.List;

public class Round {

    private final int number;
    private final List<Fixture> fixtures;

    public Round(int number, List<Fixture> fixtures) {
        this.number = number;
        this.fixtures = List.copyOf(fixtures);
    }

    public boolean isPlayed() {
        return fixtures.stream().allMatch(Fixture::isPlayed);
    }

    public int getNumber() { return number; }
    public List<Fixture> getFixtures() { return fixtures; }

    @Override
    public String toString() {
        return "Round " + number;
    }
}
