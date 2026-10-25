package org.example.console;

import org.example.league.LeagueTable;
import org.example.league.StandingRow;

import java.util.List;

public class LeagueTablePrinter {

    public void print(String title, LeagueTable table) {
        List<StandingRow> standings = table.getStandings();
        System.out.println();
        System.out.println("=== " + title + " ===");
        System.out.printf("%3s  %-20s %3s %3s %3s %3s %4s %4s %4s %4s  %s%n",
                "Pos", "Team", "P", "W", "D", "L", "GF", "GA", "GD", "Pts", "Form");
        for (int i = 0; i < standings.size(); i++) {
            StandingRow row = standings.get(i);
            System.out.printf("%3d  %-20s %3d %3d %3d %3d %4d %4d %+4d %4d  %s%n",
                    i + 1, row.getTeam().getName(), row.getPlayed(), row.getWon(), row.getDrawn(), row.getLost(),
                    row.getGoalsFor(), row.getGoalsAgainst(), row.getGoalDifference(), row.getPoints(), row.getForm());
        }
    }
}
