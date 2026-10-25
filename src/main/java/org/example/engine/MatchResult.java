package org.example.engine;

import org.example.engine.event.EventDisplayMode;
import org.example.engine.event.MatchEvent;
import org.example.model.Player;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// Everything that is left after the final whistle. Immutable.
public class MatchResult {

    public static final int POINTS_FOR_WIN = 3;
    public static final int POINTS_FOR_DRAW = 1;

    private final Team homeTeam;
    private final Team awayTeam;
    private final Referee referee;
    private final Score score;
    private final List<Player> homeLineup;
    private final List<Player> awayLineup;
    private final TeamMatchStats homeStats;
    private final TeamMatchStats awayStats;
    private final List<MatchEvent> events;
    private final List<PlayerMatchStats> homePlayerStats;
    private final List<PlayerMatchStats> awayPlayerStats;

    public MatchResult(MatchContext context, Referee referee) {
        this.homeTeam = context.getHome().getTeam();
        this.awayTeam = context.getAway().getTeam();
        this.referee = referee;
        this.score = context.score();
        this.homeLineup = context.getHome().getStartingEleven();
        this.awayLineup = context.getAway().getStartingEleven();
        this.homeStats = TeamMatchStats.from(context, context.getHome());
        this.awayStats = TeamMatchStats.from(context, context.getAway());
        this.events = context.events().getEvents();
        this.homePlayerStats = List.copyOf(context.playerStatsOf(homeTeam));
        this.awayPlayerStats = List.copyOf(context.playerStatsOf(awayTeam));
    }

    // ----- Result -----

    public boolean isHomeWin() { return score.home() > score.away(); }
    public boolean isAwayWin() { return score.away() > score.home(); }
    public boolean isDraw() { return score.home() == score.away(); }

    public Optional<Team> getWinner() {
        if (isHomeWin()) return Optional.of(homeTeam);
        if (isAwayWin()) return Optional.of(awayTeam);
        return Optional.empty();
    }

    public int pointsFor(Team team) {
        if (team != homeTeam && team != awayTeam) {
            throw new IllegalArgumentException(team.getName() + " did not play in this match");
        }
        if (isDraw()) {
            return POINTS_FOR_DRAW;
        }
        return getWinner().orElseThrow() == team ? POINTS_FOR_WIN : 0;
    }

    // ----- Player statistics -----

    public List<PlayerMatchStats> getHomePlayerStats() {
        return homePlayerStats;
    }

    public List<PlayerMatchStats> getAwayPlayerStats() {
        return awayPlayerStats;
    }

    public List<PlayerMatchStats> getAllPlayerStats() {
        return Stream.concat(homePlayerStats.stream(), awayPlayerStats.stream()).toList();
    }

    public Optional<PlayerMatchStats> statsOf(Player player) {
        return getAllPlayerStats().stream().filter(stats -> stats.getPlayer() == player).findFirst();
    }

    // The best rating of the match; with equal ratings, more goals and assists win.
    public Optional<PlayerMatchStats> getManOfTheMatch() {
        return getAllPlayerStats().stream()
                .filter(PlayerMatchStats::isRated)
                .max(Comparator.comparingDouble(PlayerMatchStats::getRating)
                        .thenComparingInt(stats -> stats.getGoals() + stats.getAssists()));
    }

    // ----- Events -----

    public List<MatchEvent> getEvents() {
        return events;
    }

    public List<MatchEvent> getEvents(EventDisplayMode mode) {
        return events.stream().filter(mode::shouldDisplay).toList();
    }

    public <T extends MatchEvent> List<T> getEventsOfType(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    // ----- Getters -----

    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public Referee getReferee() { return referee; }
    public Score getScore() { return score; }
    public List<Player> getHomeLineup() { return homeLineup; }
    public List<Player> getAwayLineup() { return awayLineup; }
    public TeamMatchStats getHomeStats() { return homeStats; }
    public TeamMatchStats getAwayStats() { return awayStats; }

    @Override
    public String toString() {
        return homeTeam.getName() + " " + score + " " + awayTeam.getName();
    }
}
