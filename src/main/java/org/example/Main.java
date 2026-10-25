package org.example;

import org.example.engine.MatchContext;
import org.example.engine.MatchTeam;
import org.example.engine.event.CornerEvent;
import org.example.engine.event.EventDisplayMode;
import org.example.engine.event.FoulEvent;
import org.example.engine.event.GoalEvent;
import org.example.engine.event.MatchEvent;
import org.example.engine.event.PeriodEvent;
import org.example.engine.event.ShotEvent;
import org.example.engine.phase.KickoffState;
import org.example.engine.phase.PhaseRunner;
import org.example.engine.phase.PhaseState;
import org.example.engine.phase.PhaseSummary;
import org.example.engine.random.RandomProvider;
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

        MatchContext match = simulateMatch(barcelona, real, new RandomProvider(42));
        printMatch(match, EventDisplayMode.KEY);

        testManyMatches(teams, 1000);
    }

    // ----- A temporary match loop. Stage 16 moves it into the Match class. -----

    private static MatchContext simulateMatch(Team homeTeam, Team awayTeam, RandomProvider random) {
        MatchTeam home = MatchTeam.prepare(homeTeam, true);
        MatchTeam away = MatchTeam.prepare(awayTeam, false);
        MatchContext context = new MatchContext(home, away, random);
        PhaseRunner runner = new PhaseRunner();

        MatchTeam firstKickoff = random.chance(0.5) ? home : away;

        context.giveBallTo(firstKickoff);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.KICK_OFF, context.score()));
        playHalf(context, runner);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.HALF_TIME, context.score()));

        context.clock().startSecondHalf();
        context.giveBallTo(context.opponentOf(firstKickoff));
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.SECOND_HALF, context.score()));
        playHalf(context, runner);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.FULL_TIME, context.score()));

        return context;
    }

    private static void playHalf(MatchContext context, PhaseRunner runner) {
        PhaseState next = new KickoffState();
        while (!context.clock().isPeriodOver()) {
            PhaseSummary phase = runner.run(next, context);
            if (context.clock().isRegulationTimeOver() && !context.clock().isStoppageTimeAnnounced()) {
                context.clock().announceStoppageTime(context.random());
            }
            next = phase.nextStart();
        }
    }

    // ----- Printing -----

    private static void printMatch(MatchContext match, EventDisplayMode mode) {
        MatchTeam home = match.getHome();
        MatchTeam away = match.getAway();

        System.out.println("=== " + home.getName() + " " + match.score() + " " + away.getName() + " (" + mode + " events) ===");
        for (MatchEvent event : match.events().getEvents(mode)) {
            System.out.println(event);
        }

        System.out.println();
        System.out.printf("%-14s %8s %8s%n", "", home.getTeam().getShortName(), away.getTeam().getShortName());
        printStat("Goals", match.score().home(), match.score().away());
        printStat("Shots", shots(match, home, false), shots(match, away, false));
        printStat("On target", shots(match, home, true), shots(match, away, true));
        printStat("Corners", count(match, CornerEvent.class, home), count(match, CornerEvent.class, away));
        printStat("Fouls", count(match, FoulEvent.class, home), count(match, FoulEvent.class, away));
        printStat("Possession %", match.possessionPercent(home), match.possessionPercent(away));
    }

    private static void printStat(String name, int home, int away) {
        System.out.printf("%-14s %8d %8d%n", name, home, away);
    }

    private static int shots(MatchContext match, MatchTeam team, boolean onTargetOnly) {
        return (int) match.events().getEventsOfType(ShotEvent.class).stream()
                .filter(shot -> shot.getTeam() == team.getTeam())
                .filter(shot -> !onTargetOnly || shot.isOnTarget())
                .count();
    }

    private static int count(MatchContext match, Class<? extends MatchEvent> type, MatchTeam team) {
        return (int) match.events().getEventsOfType(type).stream()
                .filter(event -> event.getTeam() == team.getTeam())
                .count();
    }

    // ----- Calibration: are the numbers realistic over many matches? -----

    private static void testManyMatches(List<Team> teams, int matches) {
        RandomProvider random = new RandomProvider(2026);
        double goals = 0, shots = 0, onTarget = 0, corners = 0, fouls = 0, headers = 0, penalties = 0;
        int homeWins = 0, draws = 0, awayWins = 0;

        for (int i = 0; i < matches; i++) {
            Team home = random.pickOne(teams);
            Team away;
            do {
                away = random.pickOne(teams);
            } while (away == home);

            MatchContext match = simulateMatch(home, away, random);
            int homeGoals = match.score().home();
            int awayGoals = match.score().away();

            goals += homeGoals + awayGoals;
            shots += match.events().getEventsOfType(ShotEvent.class).size();
            onTarget += match.events().getEventsOfType(ShotEvent.class).stream().filter(ShotEvent::isOnTarget).count();
            corners += match.events().getEventsOfType(CornerEvent.class).size();
            fouls += match.events().getEventsOfType(FoulEvent.class).size();
            for (GoalEvent goal : match.events().getEventsOfType(GoalEvent.class)) {
                if (goal.getGoalType() == GoalEvent.GoalType.HEADER) headers++;
                if (goal.getGoalType() == GoalEvent.GoalType.PENALTY) penalties++;
            }
            if (homeGoals > awayGoals) homeWins++;
            else if (homeGoals == awayGoals) draws++;
            else awayWins++;
        }

        System.out.println();
        System.out.println("=== Averages over " + matches + " matches (real football in brackets) ===");
        System.out.printf("Goals per match:     %.2f   (2.5 - 3.0)%n", goals / matches);
        System.out.printf("Shots per match:     %.1f   (22 - 28)%n", shots / matches);
        System.out.printf("On target per match: %.1f    (8 - 10)%n", onTarget / matches);
        System.out.printf("Corners per match:   %.1f    (9 - 11)%n", corners / matches);
        System.out.printf("Fouls per match:     %.1f   (20 - 26)%n", fouls / matches);
        System.out.printf("Headed goals:        %.0f%%     (about 15%%)%n", 100 * headers / goals);
        System.out.printf("Penalty goals:       %.0f%%     (about 10%%)%n", 100 * penalties / goals);
        System.out.printf("Home / draw / away:  %.0f%% / %.0f%% / %.0f%%   (45%% / 25%% / 30%%)%n",
                100.0 * homeWins / matches, 100.0 * draws / matches, 100.0 * awayWins / matches);
    }

    private static Team findTeam(List<Team> teams, String name) {
        return teams.stream()
                .filter(team -> team.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + name));
    }
}
