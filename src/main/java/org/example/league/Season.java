package org.example.league;

import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Season {

    private static final int NOT_SAVED = 0;

    private int id = NOT_SAVED;
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

    private Season(int id, String name, long seed, List<Team> teams, List<Referee> referees,
                   List<Round> rounds, RandomProvider random) {
        this.id = id;
        this.name = name;
        this.seed = seed;
        this.random = random;
        this.teams = List.copyOf(teams);
        this.referees = List.copyOf(referees);
        this.rounds = List.copyOf(rounds);
    }

    // A new season: the schedule and the referees are decided at once, from the seed.
    public static Season create(String name, List<Team> teams, List<Referee> referees, long seed) {
        return new Season(name, seed, teams, referees);
    }

    // A season saved in the database: the schedule, the referees and the scores come back as they were.
    // The random generator is restarted from the seed and the number of matches played, so continuing
    // a season is still reproducible (but not the same as playing it in one run).
    public static Season restore(int id, String name, long seed, List<Team> teams, List<Referee> referees,
                                 List<SavedMatch> schedule) {
        Map<Integer, Team> teamsById = teams.stream().collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Integer, Referee> refereesById = referees.stream().collect(Collectors.toMap(Referee::getId, Function.identity()));

        Map<Integer, List<Fixture>> fixturesByRound = new TreeMap<>();
        long played = 0;
        for (SavedMatch saved : schedule) {
            Fixture fixture = new Fixture(saved.round(), find(teamsById, saved.homeTeamId(), "team"),
                    find(teamsById, saved.awayTeamId(), "team"));
            fixture.assignId(saved.id());
            fixture.assignReferee(find(refereesById, saved.refereeId(), "referee"));
            if (saved.isPlayed()) {
                fixture.restoreScore(saved.score());
                played++;
            }
            fixturesByRound.computeIfAbsent(saved.round(), round -> new ArrayList<>()).add(fixture);
        }

        List<Round> rounds = fixturesByRound.entrySet().stream()
                .map(entry -> new Round(entry.getKey(), entry.getValue()))
                .toList();
        return new Season(id, name, seed, teams, referees, rounds, new RandomProvider(seed + played));
    }

    private static <T> T find(Map<Integer, T> byId, int id, String what) {
        T found = byId.get(id);
        if (found == null) {
            throw new IllegalStateException("The saved season uses a " + what + " that does not exist: id " + id);
        }
        return found;
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

    // The table is always built from the matches already played, so it can never disagree with them.
    public LeagueTable getTable() {
        LeagueTable table = new LeagueTable(teams);
        rounds.stream()
                .flatMap(round -> round.getFixtures().stream())
                .filter(Fixture::isPlayed)
                .forEach(fixture -> table.record(fixture.getHomeTeam(), fixture.getAwayTeam(),
                        fixture.getScore().home(), fixture.getScore().away()));
        return table;
    }

    public List<Fixture> getFixturesOf(Team team) {
        return rounds.stream()
                .flatMap(round -> round.getFixtures().stream())
                .filter(fixture -> fixture.involves(team))
                .toList();
    }

    // The id the database gave this season. Set once, when the season is saved.
    public void assignId(int id) {
        if (this.id != NOT_SAVED) {
            throw new IllegalStateException("This season already has the id " + this.id);
        }
        this.id = id;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public long getSeed() { return seed; }
    public RandomProvider getRandom() { return random; }
    public List<Team> getTeams() { return teams; }
    public List<Referee> getReferees() { return referees; }
    public List<Round> getRounds() { return rounds; }
    public int getNumberOfRounds() { return rounds.size(); }
}
