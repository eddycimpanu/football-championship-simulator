package org.example;

import org.example.league.Fixture;
import org.example.league.Round;
import org.example.league.Season;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    private static final String SEASON_NAME = "All-Star League 2026/27";
    private static final long SEED = 2026;

    public static void main(String[] args) {
        LeagueLoader loader = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load()));
        List<Team> teams = loader.loadTeams();
        List<Referee> referees = loader.loadReferees();

        Season season = Season.create(SEASON_NAME, teams, referees, SEED);
        System.out.println("=== " + season.getName() + ": " + season.getNumberOfRounds() + " rounds ===");

        printRound(season.getRound(1));
        printRound(season.getRound(20));
        printTeamSchedule(season, teams.get(0));
        checkSchedule(season);
    }

    private static void printRound(Round round) {
        System.out.println();
        System.out.println(round + ":");
        for (Fixture fixture : round.getFixtures()) {
            System.out.printf("  %-20s vs %-20s referee %s%n",
                    fixture.getHomeTeam().getName(), fixture.getAwayTeam().getName(), fixture.getReferee().getFullName());
        }
    }

    private static void printTeamSchedule(Season season, Team team) {
        System.out.println();
        System.out.println(team.getName() + " schedule (H = home, A = away):");
        StringBuilder line = new StringBuilder("  ");
        for (Fixture fixture : season.getFixturesOf(team)) {
            line.append(fixture.getHomeTeam() == team ? "H" : "A");
        }
        System.out.println(line);
    }

    // Every rule of a fair schedule, checked automatically
    private static void checkSchedule(Season season) {
        List<Team> teams = season.getTeams();
        boolean eachPairTwiceWithSwappedHome = true;
        boolean nobodyTwiceInARound = true;
        boolean nineteenHomeGames = true;
        boolean differentRefereesPerRound = true;
        int longestHomeOrAwayRun = 0;
        int mostGamesSameRefereeSameTeam = 0;

        Set<String> homeAwayPairs = new HashSet<>();
        Map<String, Integer> refereeWithTeam = new HashMap<>();
        for (Round round : season.getRounds()) {
            Set<Team> playing = new HashSet<>();
            Set<Referee> refs = new HashSet<>();
            for (Fixture fixture : round.getFixtures()) {
                nobodyTwiceInARound &= playing.add(fixture.getHomeTeam()) & playing.add(fixture.getAwayTeam());
                differentRefereesPerRound &= refs.add(fixture.getReferee());
                eachPairTwiceWithSwappedHome &= homeAwayPairs.add(fixture.getHomeTeam().getName() + ">" + fixture.getAwayTeam().getName());
                for (Team team : List.of(fixture.getHomeTeam(), fixture.getAwayTeam())) {
                    int times = refereeWithTeam.merge(fixture.getReferee().getLastName() + "/" + team.getName(), 1, Integer::sum);
                    mostGamesSameRefereeSameTeam = Math.max(mostGamesSameRefereeSameTeam, times);
                }
            }
        }
        eachPairTwiceWithSwappedHome &= homeAwayPairs.size() == teams.size() * (teams.size() - 1);

        for (Team team : teams) {
            List<Fixture> fixtures = season.getFixturesOf(team);
            long home = fixtures.stream().filter(f -> f.getHomeTeam() == team).count();
            nineteenHomeGames &= home == teams.size() - 1;
            int run = 0;
            Boolean lastWasHome = null;
            for (Fixture fixture : fixtures) {
                boolean atHome = fixture.getHomeTeam() == team;
                run = (lastWasHome != null && lastWasHome == atHome) ? run + 1 : 1;
                lastWasHome = atHome;
                longestHomeOrAwayRun = Math.max(longestHomeOrAwayRun, run);
            }
        }

        System.out.println();
        System.out.println("=== Schedule checks ===");
        System.out.println("Matches:                          " + season.getRounds().size() * season.getRound(1).getFixtures().size() + " (expected 380)");
        System.out.println("Every pair twice, home swapped:   " + ok(eachPairTwiceWithSwappedHome));
        System.out.println("Nobody plays twice in a round:    " + ok(nobodyTwiceInARound));
        System.out.println("19 home games for every team:     " + ok(nineteenHomeGames));
        System.out.println("10 different referees per round:  " + ok(differentRefereesPerRound));
        System.out.println("Longest home or away run:         " + longestHomeOrAwayRun + " games");
        System.out.println("Same referee with same team, max: " + mostGamesSameRefereeSameTeam + " of 38 games");
    }

    private static String ok(boolean value) {
        return value ? "OK" : "FAILED";
    }
}
