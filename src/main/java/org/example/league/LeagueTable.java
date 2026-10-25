package org.example.league;

import org.example.engine.MatchResult;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// The league table. Order: points, goal difference, goals scored, then head-to-head.
public class LeagueTable {

    // Points, then goal difference, then goals scored: a chained comparator
    private static final Comparator<StandingRow> BY_POINTS_GOAL_DIFFERENCE_GOALS =
            Comparator.comparingInt(StandingRow::getPoints)
                    .thenComparingInt(StandingRow::getGoalDifference)
                    .thenComparingInt(StandingRow::getGoalsFor)
                    .reversed();

    private final Map<Team, StandingRow> rows = new LinkedHashMap<>();
    private final List<Score> results = new ArrayList<>();

    public LeagueTable(List<Team> teams) {
        for (Team team : teams) {
            rows.put(team, new StandingRow(team));
        }
    }

    public void record(MatchResult result) {
        record(result.getHomeTeam(), result.getAwayTeam(), result.getScore().home(), result.getScore().away());
    }

    public void record(Team home, Team away, int homeGoals, int awayGoals) {
        rowOf(home).addResult(homeGoals, awayGoals);
        rowOf(away).addResult(awayGoals, homeGoals);
        results.add(new Score(home, away, homeGoals, awayGoals));
    }

    // The table in order. Teams level on points, goal difference and goals are split by head-to-head.
    public List<StandingRow> getStandings() {
        List<StandingRow> sorted = new ArrayList<>(rows.values());
        sorted.sort(BY_POINTS_GOAL_DIFFERENCE_GOALS);

        List<StandingRow> standings = new ArrayList<>();
        int start = 0;
        while (start < sorted.size()) {
            int end = start + 1;
            while (end < sorted.size() && BY_POINTS_GOAL_DIFFERENCE_GOALS.compare(sorted.get(start), sorted.get(end)) == 0) {
                end++;
            }
            List<StandingRow> level = new ArrayList<>(sorted.subList(start, end));
            if (level.size() > 1) {
                level.sort(headToHead(level));
            }
            standings.addAll(level);
            start = end;
        }
        return standings;
    }

    // A mini table with only the matches between the teams that are level; then alphabetical as a last resort.
    private Comparator<StandingRow> headToHead(List<StandingRow> level) {
        Set<Team> group = level.stream().map(StandingRow::getTeam).collect(Collectors.toSet());
        Map<Team, StandingRow> mini = new LinkedHashMap<>();
        for (Team team : group) {
            mini.put(team, new StandingRow(team));
        }
        for (Score score : results) {
            if (group.contains(score.home()) && group.contains(score.away())) {
                mini.get(score.home()).addResult(score.homeGoals(), score.awayGoals());
                mini.get(score.away()).addResult(score.awayGoals(), score.homeGoals());
            }
        }
        return Comparator.<StandingRow, StandingRow>comparing(row -> mini.get(row.getTeam()), BY_POINTS_GOAL_DIFFERENCE_GOALS)
                .thenComparing(row -> row.getTeam().getName());
    }

    public int positionOf(Team team) {
        List<StandingRow> standings = getStandings();
        for (int i = 0; i < standings.size(); i++) {
            if (standings.get(i).getTeam() == team) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException(team.getName() + " is not in this league");
    }

    public StandingRow rowOf(Team team) {
        StandingRow row = rows.get(team);
        if (row == null) {
            throw new IllegalArgumentException(team.getName() + " is not in this league");
        }
        return row;
    }

    public StandingRow getLeader() {
        return getStandings().get(0);
    }

    private record Score(Team home, Team away, int homeGoals, int awayGoals) {
    }
}
