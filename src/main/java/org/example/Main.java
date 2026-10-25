package org.example;

import org.example.engine.MatchMinute;
import org.example.engine.Score;
import org.example.engine.event.CardEvent;
import org.example.engine.event.CornerEvent;
import org.example.engine.event.EventDisplayMode;
import org.example.engine.event.FoulEvent;
import org.example.engine.event.GoalEvent;
import org.example.engine.event.InjuryEvent;
import org.example.engine.event.MatchEvent;
import org.example.engine.event.MatchEventLog;
import org.example.engine.event.OffsideEvent;
import org.example.engine.event.PeriodEvent;
import org.example.engine.event.SaveEvent;
import org.example.engine.event.ShotEvent;
import org.example.engine.event.SubstitutionEvent;
import org.example.model.Player;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Team> teams = new LeagueLoader(new ConnectionProvider(DatabaseConfig.load())).loadTeams();
        Team barcelona = findTeam(teams, "Barcelona");
        Team real = findTeam(teams, "Real Madrid");

        MatchEventLog log = buildFakeElClasico(barcelona, real);

        printEvents(log, EventDisplayMode.ALL, barcelona, real);
        printEvents(log, EventDisplayMode.KEY, barcelona, real);

        System.out.println();
        System.out.println("Goals in the match: " + log.getEventsOfType(GoalEvent.class).size());
        System.out.println("Total events: " + log.size());
    }

    private static MatchEventLog buildFakeElClasico(Team barcelona, Team real) {
        Player messi = findPlayer(barcelona, "Messi");
        Player xavi = findPlayer(barcelona, "Xavi");
        Player puyol = findPlayer(barcelona, "Puyol");
        Player marquez = findPlayer(barcelona, "Márquez");
        Player ronaldo = findPlayer(real, "Ronaldo");
        Player ramos = findPlayer(real, "Ramos");
        Player casillas = findPlayer(real, "Casillas");
        Player modric = findPlayer(real, "Modrić");

        Score score = Score.ZERO;
        MatchEventLog log = new MatchEventLog();

        log.add(new PeriodEvent(MatchMinute.of(1), PeriodEvent.Kind.KICK_OFF, score));
        log.add(new FoulEvent(MatchMinute.of(12), real, ramos, messi));
        log.add(new CardEvent(MatchMinute.of(12), real, ramos, CardEvent.CardType.YELLOW));
        log.add(new ShotEvent(MatchMinute.of(13), barcelona, messi, true));
        log.add(new SaveEvent(MatchMinute.of(13), real, casillas, messi));
        log.add(new CornerEvent(MatchMinute.of(14), barcelona));

        score = score.homeScores();
        log.add(new GoalEvent(MatchMinute.of(34), barcelona, messi, xavi, GoalEvent.GoalType.NORMAL, score));

        log.add(new PeriodEvent(new MatchMinute(45, 2), PeriodEvent.Kind.HALF_TIME, score));
        log.add(new PeriodEvent(MatchMinute.of(46), PeriodEvent.Kind.SECOND_HALF, score));
        log.add(new OffsideEvent(MatchMinute.of(58), real, ronaldo));
        log.add(new InjuryEvent(MatchMinute.of(61), barcelona, puyol));
        log.add(new SubstitutionEvent(MatchMinute.of(61), barcelona, puyol, marquez,
                SubstitutionEvent.Reason.INJURY));

        score = score.awayScores();
        log.add(new GoalEvent(MatchMinute.of(67), real, ronaldo, modric, GoalEvent.GoalType.HEADER, score));

        log.add(new CardEvent(MatchMinute.of(78), real, ramos, CardEvent.CardType.SECOND_YELLOW));

        score = score.homeScores();
        log.add(new GoalEvent(MatchMinute.of(84), barcelona, messi, null, GoalEvent.GoalType.PENALTY, score));

        log.add(new PeriodEvent(new MatchMinute(90, 3), PeriodEvent.Kind.FULL_TIME, score));
        return log;
    }

    private static void printEvents(MatchEventLog log, EventDisplayMode mode, Team home, Team away) {
        System.out.println();
        System.out.println("=== " + home.getName() + " vs " + away.getName() + " (" + mode + " events) ===");
        for (MatchEvent event : log.getEvents(mode)) {
            System.out.println(event);
        }
    }

    private static Team findTeam(List<Team> teams, String name) {
        return teams.stream()
                .filter(team -> team.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + name));
    }

    private static Player findPlayer(Team team, String lastName) {
        return team.getSquad().stream()
                .filter(player -> player.getLastName().equals(lastName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(lastName + " not found in " + team.getName()));
    }
}