package org.example.league;

import org.example.engine.Match;
import org.example.engine.MatchResult;
import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;

// One scheduled match of the season. It has a result once it has been played.
public class Fixture {

    private final int round;
    private final Team homeTeam;
    private final Team awayTeam;
    private Referee referee;
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
            throw new IllegalStateException("Already played: " + result);
        }
        result = new Match(homeTeam, awayTeam, referee, random).simulate();
        return result;
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

    void assignReferee(Referee referee) {
        this.referee = referee;
    }

    public boolean isPlayed() { return result != null; }
    public int getRound() { return round; }
    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public Referee getReferee() { return referee; }
    public MatchResult getResult() { return result; }

    @Override
    public String toString() {
        return isPlayed() ? result.toString() : homeTeam.getName() + " vs " + awayTeam.getName();
    }
}
