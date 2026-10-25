package org.example.engine;

import org.example.config.EngineConfig;
import org.example.engine.event.GoalEvent;
import org.example.engine.event.MatchEvent;
import org.example.engine.event.MatchEventLog;
import org.example.engine.random.RandomProvider;
import org.example.model.Player;

public class MatchContext {

    private final MatchTeam home;
    private final MatchTeam away;
    private final RandomProvider random;
    private final MatchClock clock = new MatchClock();
    private final MatchEventLog events = new MatchEventLog();

    private MatchTeam inPossession;
    private Score score = Score.ZERO;
    private int homePossessionSeconds;
    private int awayPossessionSeconds;

    public MatchContext(MatchTeam home, MatchTeam away, RandomProvider random) {
        this.home = home;
        this.away = away;
        this.random = random;
        this.inPossession = home;
    }

    // ----- Possession -----

    public MatchTeam attackingTeam() {
        return inPossession;
    }

    public MatchTeam defendingTeam() {
        return opponentOf(inPossession);
    }

    public MatchTeam opponentOf(MatchTeam team) {
        return team == home ? away : home;
    }

    public void switchPossession() {
        inPossession = defendingTeam();
    }

    public void giveBallTo(MatchTeam team) {
        inPossession = team;
    }

    public void addPossessionTime(MatchTeam team, int seconds) {
        if (team == home) {
            homePossessionSeconds += seconds;
        } else {
            awayPossessionSeconds += seconds;
        }
    }

    public int possessionPercent(MatchTeam team) {
        int total = homePossessionSeconds + awayPossessionSeconds;
        if (total == 0) {
            return 50;
        }
        int teamSeconds = team == home ? homePossessionSeconds : awayPossessionSeconds;
        return (int) Math.round(100.0 * teamSeconds / total);
    }

    // ----- Home advantage: > 1 when the home team attacks, < 1 when the away team attacks -----

    public double homeFactor() {
        return inPossession.isHome() ? EngineConfig.HOME_ADVANTAGE : 1 / EngineConfig.HOME_ADVANTAGE;
    }

    // ----- Events and goals -----

    public MatchMinute minute() {
        return clock.getMinute();
    }

    public void addEvent(MatchEvent event) {
        events.add(event);
    }

    public void recordGoal(MatchTeam scoringTeam, Player scorer, Player assist, GoalEvent.GoalType goalType) {
        score = scoringTeam == home ? score.homeScores() : score.awayScores();
        clock.recordGoal();
        events.add(new GoalEvent(minute(), scoringTeam.getTeam(), scorer, assist, goalType, score));
    }

    public MatchTeam getHome() { return home; }
    public MatchTeam getAway() { return away; }
    public RandomProvider random() { return random; }
    public MatchClock clock() { return clock; }
    public MatchEventLog events() { return events; }
    public Score score() { return score; }
}
