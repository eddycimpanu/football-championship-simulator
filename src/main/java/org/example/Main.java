package org.example;

import org.example.model.Player;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.Comparator;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        ConnectionProvider connectionProvider = new ConnectionProvider(DatabaseConfig.load());
        LeagueLoader leagueLoader = new LeagueLoader(connectionProvider);

        List<Team> teams = leagueLoader.loadTeams();
        List<Referee> referees = leagueLoader.loadReferees();

        printTeams(teams);
        printSquad(teams, "Barcelona");
        printReferees(referees);
    }

    private static void printTeams(List<Team> teams) {
        System.out.println("=== Teams (by average OVR) ===");
        teams.stream()
                .sorted(Comparator.comparingDouble(Team::getAverageOverall).reversed())
                .forEach(team -> System.out.printf("%-20s %-4s %-22s %2d players   avg OVR %.1f%n",
                        team.getName(),
                        team.getShortName(),
                        team.getCoach().getFullName(),
                        team.getSquadSize(),
                        team.getAverageOverall()));
    }

    private static void printSquad(List<Team> teams, String teamName) {
        Team team = teams.stream()
                .filter(t -> t.getName().equals(teamName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamName));

        System.out.println();
        System.out.println("=== " + team.getName() + " squad (by OVR) ===");
        team.getSquad().stream()
                .sorted(Comparator.comparingInt(Player::getOverall).reversed())
                .forEach(player -> System.out.printf("#%-3d %-25s %-4s %3d OVR%n",
                        player.getShirtNumber(),
                        player.getFullName(),
                        player.getPosition(),
                        player.getOverall()));
    }

    private static void printReferees(List<Referee> referees) {
        System.out.println();
        System.out.println("=== Referees ===");
        for (Referee referee : referees) {
            System.out.println(referee);
        }
    }
}