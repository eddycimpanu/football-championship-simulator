package org.example.engine;

import org.example.config.EngineConfig;
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
        MatchContext context = new MatchContext(home, away, referee, random);
        MatchTeam firstKickoff = random.chance(0.5) ? home : away;

        // First half
        context.giveBallTo(firstKickoff);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.KICK_OFF, context.score()));
        playHalf(context);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.HALF_TIME, context.score()));

        // Half-time: a short rest, then the other team kicks off
        home.recoverEnergy(EngineConfig.HALF_TIME_RECOVERY);
        away.recoverEnergy(EngineConfig.HALF_TIME_RECOVERY);
        context.clock().startSecondHalf();
        context.giveBallTo(context.opponentOf(firstKickoff));
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.SECOND_HALF, context.score()));
        SubstitutionManager.considerChanges(context, home);
        SubstitutionManager.considerChanges(context, away);
        playHalf(context);
        context.addEvent(new PeriodEvent(context.minute(), PeriodEvent.Kind.FULL_TIME, context.score()));

        // Everyone who played is now tired: their condition drops for the next rounds
        home.applyConditionLoss();
        away.applyConditionLoss();

        // Bans served, yellow cards counted, red cards turned into suspensions, injured players out
        home.applyDisciplineAfterMatch();
        away.applyDisciplineAfterMatch();
        home.applyInjuriesAfterMatch();
        away.applyInjuriesAfterMatch();

        result = new MatchResult(context, referee);
        return result;
    }

    // Phase after phase until the half is over. A phase that has started is never cut in the middle.
    // Between two phases: a possible injury, and in the second half the coaches' substitution windows.
    private void playHalf(MatchContext context) {
        PhaseState next = new KickoffState();
        int window = 0;
        while (!context.clock().isPeriodOver() && !context.isAbandoned()) {
            PhaseSummary phase = phaseRunner.run(next, context);

            InjuryDecider.afterPhase(context);

            if (context.clock().getPeriod() == MatchPeriod.SECOND_HALF) {
                while (window < EngineConfig.SUBSTITUTION_MINUTES.length
                        && context.minute().minute() >= EngineConfig.SUBSTITUTION_MINUTES[window]) {
                    SubstitutionManager.considerChanges(context, context.getHome());
                    SubstitutionManager.considerChanges(context, context.getAway());
                    window++;
                }
            }

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
