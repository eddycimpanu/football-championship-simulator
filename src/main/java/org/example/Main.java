package org.example;

import org.example.console.MatchReportPrinter;
import org.example.engine.Match;
import org.example.engine.MatchResult;
import org.example.engine.event.EventDisplayMode;
import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.List;

public class Main {

    // ----- Change these to play another match -----
    private static final String HOME_TEAM = "Barcelona";
    private static final String AWAY_TEAM = "Real Madrid";
    private static final EventDisplayMode DISPLAY_MODE = EventDisplayMode.ALL;   // ALL or KEY
    private static final Long SEED = null;   // null = a new match every run; a number = replay that match

    public static void main(String[] args) {
        LeagueLoader loader = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load()));
        List<Team> teams = loader.loadTeams();
        List<Referee> referees = loader.loadReferees();

        RandomProvider random = SEED == null ? RandomProvider.withRandomSeed() : new RandomProvider(SEED);
        Referee referee = random.pickOne(referees);

        Match match = new Match(findTeam(teams, HOME_TEAM), findTeam(teams, AWAY_TEAM), referee, random);
        MatchResult result = match.simulate();

        MatchReportPrinter printer = new MatchReportPrinter();
        printer.printLineups(result);
        printer.printEvents(result, DISPLAY_MODE);
        printer.printStats(result);

        System.out.println();
        System.out.println("Points: " + result.getHomeTeam().getName() + " " + result.pointsFor(result.getHomeTeam())
                + ", " + result.getAwayTeam().getName() + " " + result.pointsFor(result.getAwayTeam()));
        System.out.println("Seed: " + random.getSeed() + "  (set SEED to this number to replay the same match)");
    }

    private static Team findTeam(List<Team> teams, String name) {
        return teams.stream()
                .filter(team -> team.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + name
                        + ". Check the spelling, it must match the name in the database."));
    }
}
