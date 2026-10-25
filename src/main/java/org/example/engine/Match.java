package org.example.engine;

import org.example.engine.event.PeriodEvent;
import org.example.engine.phase.KickoffState;
import org.example.engine.phase.PhaseRunner;
import org.example.engine.phase.PhaseState;
import org.example.engine.phase.PhaseSummary;
import org.example.engine.random.RandomProvider;
import org.example.model.Referee;
import org.example.model.Team;

public class Match {

    private final Team homeTeam;
    private final Team awayTeam;
    private final Referee referee;
    private final RandomProvider random;
    private final PhaseRunner phaseRunner = new PhaseRunner();

    private MatchResult result;

    public Match(Team homeTeam, Team awayTeam, Referee referee, RandomProvider random) {
        if (homeTeam == null || awayTeam == null) {
            throw new IllegalArgumentException("A match needs two teams");
        }
        if (homeTeam == awayTeam) {
            throw new IllegalArgumentException(homeTeam.getName() + " cannot play against itself");
        }
        if (random == null) {
            throw new IllegalArgumentException("A match needs a random provider");
        }
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.referee = referee;
        this.random = random;
    }

    public MatchResult simulate() {
        if (result != null) {
            throw new IllegalStateException("This match has already been played: " + result);
        }

        MatchTeam home = MatchTeam.prepare(homeTeam, true);
        MatchTeam away = MatchTeam.prepare(awayTeam, false);
        MatchContext context = new MatchContext(home, away, random);
        MatchTeam firstKickoff = random.chance(0.5) ? home : away;

        // First half
        context.giveBallTo(firstKickoff);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.KICK_OFF, context.score()));
        playHalf(context);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.HALF_TIME, context.score()));

        // Second half: the other team kicks off
        context.clock().startSecondHalf();
        context.giveBallTo(context.opponentOf(firstKickoff));
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.SECOND_HALF, context.score()));
        playHalf(context);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.FULL_TIME, context.score()));

        result = new MatchResult(context, referee);
        return result;
    }

    // Phase after phase until the half is over. A phase that has started is never cut in the middle.
    private void playHalf(MatchContext context) {
        PhaseState next = new KickoffState();
        while (!context.clock().isPeriodOver()) {
            PhaseSummary phase = phaseRunner.run(next, context);
            if (context.clock().isRegulationTimeOver() && !context.clock().isStoppageTimeAnnounced()) {
                context.clock().announceStoppageTime(random);
            }
            next = phase.nextStart();
        }
    }

    public boolean isPlayed() {
        return result != null;
    }

    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public Referee getReferee() { return referee; }
}
