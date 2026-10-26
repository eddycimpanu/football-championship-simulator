package org.example.league;

import org.example.engine.Match;
import org.example.engine.MatchResult;
import org.example.engine.Score;
import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.Optional;

// One scheduled match of the season. It has a score once it has been played.
// A match played in this run also keeps its full result (events, statistics);
// a match loaded from the database only has the score, the rest stays in the database.
public class Fixture {

    private static final int NOT_SAVED = 0;

    private int id = NOT_SAVED;
    private final int round;
    private final Team homeTeam;
    private final Team awayTeam;
    private Referee referee;
    private Score score;
    private MatchResult result;

    public Fixture(int round, Team homeTeam, Team awayTeam) {
        if (homeTeam == awayTeam) {
            throw new IllegalArgumentException(homeTeam.getName() + " cannot play against itself");
        }
        this.round = round;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public MatchResult play(RandomProvider random) {
        if (isPlayed()) {
            throw new IllegalStateException("Already played: " + this);
        }
        result = new Match(homeTeam, awayTeam, referee, random).simulate();
        score = result.getScore();
        return result;
    }

    // A match that was played in an earlier run: only the score comes back from the database.
    void restoreScore(Score savedScore) {
        if (isPlayed()) {
            throw new IllegalStateException("Already played: " + this);
        }
        this.score = savedScore;
    }

    public boolean involves(Team team) {
        return homeTeam == team || awayTeam == team;
    }

    public Team opponentOf(Team team) {
        if (!involves(team)) {
            throw new IllegalArgumentException(team.getName() + " does not play in " + this);
        }
        return team == homeTeam ? awayTeam : homeTeam;
    }

    public int goalsFor(Team team) {
        requirePlayed();
        return team == homeTeam ? score.home() : score.away();
    }

    public int goalsAgainst(Team team) {
        return goalsFor(opponentOf(team));
    }

    private void requirePlayed() {
        if (!isPlayed()) {
            throw new IllegalStateException(this + " has not been played yet");
        }
    }

    // The id the database gave this match. Set once, when the schedule is saved or loaded.
    public void assignId(int id) {
        if (this.id != NOT_SAVED) {
            throw new IllegalStateException("This fixture already has the id " + this.id);
        }
        this.id = id;
    }

    void assignReferee(Referee referee) {
        this.referee = referee;
    }

    public boolean isPlayed() { return score != null; }
    public int getId() { return id; }
    public int getRound() { return round; }
    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public Referee getReferee() { return referee; }
    public Score getScore() { requirePlayed(); return score; }

    // Empty for matches loaded from the database
    public Optional<MatchResult> getResult() { return Optional.ofNullable(result); }

    @Override
    public String toString() {
        return isPlayed()
                ? homeTeam.getName() + " " + score + " " + awayTeam.getName()
                : homeTeam.getName() + " vs " + awayTeam.getName();
    }
}
