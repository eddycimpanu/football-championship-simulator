package org.example.league;

import org.example.engine.random.RandomProvider;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.List;

// Double round-robin with the circle method: one team stays in place, the others rotate.
// First half: rounds 1-19. Second half: the same pairs with home and away swapped.
public class FixtureGenerator {

    public List<Round> generate(List<Team> teams, RandomProvider random) {
        if (teams.size() < 2 || teams.size() % 2 != 0) {
            throw new IllegalArgumentException("A league needs an even number of teams, got " + teams.size());
        }
        List<Team> order = shuffled(teams, random);
        int teamCount = order.size();
        int roundsPerHalf = teamCount - 1;
        int matchesPerRound = teamCount / 2;

        List<List<Team[]>> firstHalf = new ArrayList<>();
        for (int round = 0; round < roundsPerHalf; round++) {
            List<Team[]> pairs = new ArrayList<>();
            for (int i = 0; i < matchesPerRound; i++) {
                Team first = order.get(i);
                Team second = order.get(teamCount - 1 - i);
                // Berger rule: the fixed team alternates every round; every other team moves one place
                // around the circle each round, so "first team at home on odd places" makes it alternate too
                boolean firstAtHome = (i == 0) ? round % 2 == 0 : i % 2 == 1;
                pairs.add(firstAtHome ? new Team[]{first, second} : new Team[]{second, first});
            }
            firstHalf.add(pairs);
            rotate(order);
        }

        List<Round> rounds = new ArrayList<>();
        for (int round = 0; round < roundsPerHalf; round++) {
            rounds.add(toRound(round + 1, firstHalf.get(round), false));
        }
        for (int round = 0; round < roundsPerHalf; round++) {
            rounds.add(toRound(roundsPerHalf + round + 1, firstHalf.get(round), true));
        }
        return rounds;
    }

    private Round toRound(int number, List<Team[]> pairs, boolean swapHomeAndAway) {
        List<Fixture> fixtures = new ArrayList<>();
        for (Team[] pair : pairs) {
            fixtures.add(swapHomeAndAway ? new Fixture(number, pair[1], pair[0]) : new Fixture(number, pair[0], pair[1]));
        }
        return new Round(number, fixtures);
    }

    // The first team stays; the last one moves to second place and the others shift one step.
    private void rotate(List<Team> order) {
        Team last = order.remove(order.size() - 1);
        order.add(1, last);
    }

    // Fisher-Yates shuffle with our own random provider, so the same seed gives the same schedule.
    private List<Team> shuffled(List<Team> teams, RandomProvider random) {
        List<Team> copy = new ArrayList<>(teams);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = random.nextInt(0, i);
            Team temp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, temp);
        }
        return copy;
    }
}
