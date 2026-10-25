package org.example.league;

import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.List;
import java.util.Optional;

public class Season {

    private final String name;
    private final long seed;
    private final RandomProvider random;
    private final List<Team> teams;
    private final List<Referee> referees;
    private final List<Round> rounds;

    private Season(String name, long seed, List<Team> teams, List<Referee> referees) {
        this.name = name;
        this.seed = seed;
        this.random = new RandomProvider(seed);
        this.teams = List.copyOf(teams);
        this.referees = List.copyOf(referees);
        this.rounds = new FixtureGenerator().generate(this.teams, random);
        new RefereeAssigner().assign(rounds, this.referees, random);
    }

    // A new season: the schedule and the referees are decided at once, from the seed.
    public static Season create(String name, List<Team> teams, List<Referee> referees, long seed) {
        return new Season(name, seed, teams, referees);
    }

    public Optional<Round> getNextRound() {
        return rounds.stream().filter(round -> !round.isPlayed()).findFirst();
    }

    public int getCurrentRoundNumber() {
        return getNextRound().map(Round::getNumber).orElse(rounds.size());
    }

    public boolean isFinished() {
        return getNextRound().isEmpty();
    }

    public Round getRound(int number) {
        if (number < 1 || number > rounds.size()) {
            throw new IllegalArgumentException("Round must be between 1 and " + rounds.size() + ", got " + number);
        }
        return rounds.get(number - 1);
    }

    public List<Fixture> getFixturesOf(Team team) {
        return rounds.stream()
                .flatMap(round -> round.getFixtures().stream())
                .filter(fixture -> fixture.involves(team))
                .toList();
    }

    public String getName() { return name; }
    public long getSeed() { return seed; }
    public RandomProvider getRandom() { return random; }
    public List<Team> getTeams() { return teams; }
    public List<Referee> getReferees() { return referees; }
    public List<Round> getRounds() { return rounds; }
    public int getNumberOfRounds() { return rounds.size(); }
}
