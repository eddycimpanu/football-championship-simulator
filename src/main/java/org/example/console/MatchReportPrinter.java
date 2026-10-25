package org.example.console;

import org.example.engine.MatchResult;
import org.example.engine.TeamMatchStats;
import org.example.engine.event.EventDisplayMode;
import org.example.engine.event.MatchEvent;
import org.example.model.Player;
import org.example.model.Team;

import java.util.List;
import java.util.function.ToIntFunction;

public class MatchReportPrinter {

    public void printLineups(MatchResult result) {
        printLineup(result.getHomeTeam(), result.getHomeLineup());
        printLineup(result.getAwayTeam(), result.getAwayLineup());
    }

    private void printLineup(Team team, List<Player> lineup) {
        System.out.println("=== " + team.getName() + " (" + team.getCoach().getPreferredFormation()
                + ", coach " + team.getCoach().getFullName() + ") ===");
        for (Player player : lineup) {
            System.out.printf("  #%-3d %-26s %-4s %3d OVR%n",
                    player.getShirtNumber(), player.getFullName(), player.getPosition(), player.getOverall());
        }
        System.out.println();
    }

    public void printEvents(MatchResult result, EventDisplayMode mode) {
        String referee = result.getReferee() == null ? "" : ", referee " + result.getReferee().getFullName();
        System.out.println("=== " + result + " (" + mode + " events" + referee + ") ===");
        for (MatchEvent event : result.getEvents(mode)) {
            System.out.println(event);
        }
    }

    public void printStats(MatchResult result) {
        TeamMatchStats home = result.getHomeStats();
        TeamMatchStats away = result.getAwayStats();

        System.out.println();
        System.out.printf("%-14s %8s %8s%n", "", result.getHomeTeam().getShortName(), result.getAwayTeam().getShortName());
        printStat("Goals", home, away, TeamMatchStats::goals);
        printStat("Shots", home, away, TeamMatchStats::shots);
        printStat("On target", home, away, TeamMatchStats::shotsOnTarget);
        printStat("Corners", home, away, TeamMatchStats::corners);
        printStat("Fouls", home, away, TeamMatchStats::fouls);
        printStat("Possession %", home, away, TeamMatchStats::possessionPercent);
    }

    private void printStat(String name, TeamMatchStats home, TeamMatchStats away, ToIntFunction<TeamMatchStats> value) {
        System.out.printf("%-14s %8d %8d%n", name, value.applyAsInt(home), value.applyAsInt(away));
    }
}
