package org.example.league;

import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Different referees in every round, and each referee as rarely as possible with the same teams.
public class RefereeAssigner {

    private final Map<Referee, Map<Team, Integer>> history = new HashMap<>();

    public void assign(List<Round> rounds, List<Referee> referees, RandomProvider random) {
        int matchesPerRound = rounds.get(0).getFixtures().size();
        if (referees.size() < matchesPerRound) {
            throw new IllegalArgumentException("Every round needs " + matchesPerRound + " referees, only "
                    + referees.size() + " available");
        }
        for (Round round : rounds) {
            Set<Referee> usedThisRound = new HashSet<>();
            for (Fixture fixture : round.getFixtures()) {
                Referee chosen = leastUsedWith(fixture, referees, usedThisRound, random);
                fixture.assignReferee(chosen);
                usedThisRound.add(chosen);
                remember(chosen, fixture.getHomeTeam());
                remember(chosen, fixture.getAwayTeam());
            }
        }
    }

    // The free referee who has refereed these two teams the fewest times; ties are broken at random.
    private Referee leastUsedWith(Fixture fixture, List<Referee> referees, Set<Referee> used, RandomProvider random) {
        List<Referee> best = new ArrayList<>();
        int bestScore = Integer.MAX_VALUE;
        for (Referee referee : referees) {
            if (used.contains(referee)) {
                continue;
            }
            int score = timesWith(referee, fixture.getHomeTeam()) + timesWith(referee, fixture.getAwayTeam());
            if (score < bestScore) {
                bestScore = score;
                best.clear();
            }
            if (score == bestScore) {
                best.add(referee);
            }
        }
        return random.pickOne(best);
    }

    private int timesWith(Referee referee, Team team) {
        return history.getOrDefault(referee, Map.of()).getOrDefault(team, 0);
    }

    private void remember(Referee referee, Team team) {
        history.computeIfAbsent(referee, r -> new HashMap<>()).merge(team, 1, Integer::sum);
    }
}
