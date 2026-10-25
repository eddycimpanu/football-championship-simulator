package org.example.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.config.EngineConfig;
import org.example.engine.event.GoalEvent;
import org.example.engine.event.FoulEvent;
import org.example.engine.event.OffsideEvent;
import org.example.engine.event.SaveEvent;
import org.example.engine.event.ShotEvent;
import org.example.engine.event.MatchEvent;
import org.example.engine.event.MatchEventLog;
import org.example.engine.random.RandomProvider;
import org.example.model.Player;
import org.example.model.Team;
import org.example.model.PlayerAttributes;
import org.example.model.Referee;

public class MatchContext {

    private final MatchTeam home;
    private final MatchTeam away;
    private final Referee referee;
    private final RandomProvider random;
    private final MatchClock clock = new MatchClock();
    private final MatchEventLog events = new MatchEventLog();

    private MatchTeam inPossession;
    private Score score = Score.ZERO;
    private int homePossessionSeconds;
    private int awayPossessionSeconds;

    private final Map<Player, PlayerMatchStats> playerStats = new LinkedHashMap<>();

    public MatchContext(MatchTeam home, MatchTeam away, Referee referee, RandomProvider random) {
        this.home = home;
        this.away = away;
        this.referee = referee;
        this.random = random;
        this.inPossession = home;
    }

    // ----- Time: the clock moves on and the players on the pitch get tired -----

    public void passTime(int seconds) {
        clock.advance(seconds);
        home.drainEnergy(seconds);
        away.drainEnergy(seconds);
    }

    // The attributes of a player right now, fatigue included.
    public PlayerAttributes attributes(Player player) {
        return teamOf(player).attributesOf(player);
    }

    public MatchTeam teamOf(Player player) {
        return home.has(player) ? home : away;
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

    // Home advantage plus red cards: a team with fewer players is weaker.
    public double matchupFactor() {
        return homeFactor() * attackingTeam().manpowerFactor() / defendingTeam().manpowerFactor();
    }

    // ----- Referee -----

    public double refereeStrictness() {
        return referee == null ? EngineConfig.AVERAGE_REFEREE_STRICTNESS : referee.getStrictness();
    }

    // Extremely rare: a team with fewer than 7 players cannot go on.
    public boolean isAbandoned() {
        return home.playersOnPitch() < EngineConfig.MIN_PLAYERS_ON_PITCH
                || away.playersOnPitch() < EngineConfig.MIN_PLAYERS_ON_PITCH;
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

        if (goalType == GoalEvent.GoalType.OWN_GOAL) {
            statsOf(scorer).addOwnGoal();
        } else {
            statsOf(scorer).addGoal();
        }
        if (assist != null) {
            statsOf(assist).addAssist();
        }
        statsOf(opponentOf(scoringTeam).getGoalkeeper()).addGoalConceded();
    }

    // ----- Records: the event for the match report and the number for the statistics, in one call -----

    public void recordShot(MatchTeam team, Player shooter, boolean onTarget) {
        events.add(new ShotEvent(minute(), team.getTeam(), shooter, onTarget));
        statsOf(shooter).addShot(onTarget);
    }

    public void recordSave(MatchTeam goalkeeperTeam, Player goalkeeper, Player shooter) {
        events.add(new SaveEvent(minute(), goalkeeperTeam.getTeam(), goalkeeper, shooter));
        statsOf(goalkeeper).addSave();
    }

    public void recordFoul(MatchTeam foulerTeam, Player fouler, Player fouled) {
        events.add(new FoulEvent(minute(), foulerTeam.getTeam(), fouler, fouled));
        statsOf(fouler).addFoulCommitted();
        statsOf(fouled).addFoulSuffered();
    }

    public void recordOffside(MatchTeam team, Player player) {
        events.add(new OffsideEvent(minute(), team.getTeam(), player));
        statsOf(player).addOffside();
    }

    public void recordTackle(Player player) {
        statsOf(player).addTackle();
    }

    public void recordInterception(Player player) {
        statsOf(player).addInterception();
    }

    public void recordKeyPass(Player player) {
        statsOf(player).addKeyPass();
    }

    // ----- Player statistics -----

    public PlayerMatchStats statsOf(Player player) {
        return playerStats.computeIfAbsent(player,
                p -> new PlayerMatchStats(p, teamOf(p).getTeam(), false, statsMinute()));
    }

    void registerStartingElevens() {
        for (MatchTeam team : List.of(home, away)) {
            for (Player player : team.getStartingEleven()) {
                playerStats.put(player, new PlayerMatchStats(player, team.getTeam(), true, 0));
            }
        }
    }

    void playerEnters(Player player) {
        statsOf(player);
    }

    void playerLeaves(Player player) {
        statsOf(player).leave(statsMinute());
    }

    void finishPlayerStats() {
        playerStats.values().forEach(stats -> {
            boolean homePlayer = stats.getTeam() == home.getTeam();
            int goalsFor = homePlayer ? score.home() : score.away();
            int goalsAgainst = homePlayer ? score.away() : score.home();
            stats.finish(goalsFor, goalsAgainst);
        });
    }

    public List<PlayerMatchStats> playerStatsOf(Team team) {
        List<PlayerMatchStats> result = new ArrayList<>();
        for (PlayerMatchStats stats : playerStats.values()) {
            if (stats.getTeam() == team) {
                result.add(stats);
            }
        }
        return result;
    }

    // Minute used for minutes played: changes made at half-time count as 45'.
    private int statsMinute() {
        int minute = clock.getMinute().minute();
        return minute == 46 ? 45 : Math.min(90, minute);
    }

    public MatchTeam getHome() { return home; }
    public MatchTeam getAway() { return away; }
    public Referee getReferee() { return referee; }
    public RandomProvider random() { return random; }
    public MatchClock clock() { return clock; }
    public MatchEventLog events() { return events; }
    public Score score() { return score; }
}
