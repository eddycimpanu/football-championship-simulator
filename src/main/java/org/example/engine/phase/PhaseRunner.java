package org.example.engine.phase;

import org.example.engine.MatchContext;
import org.example.engine.MatchTeam;

public class PhaseRunner {

    private static final int MAX_STATES_PER_PHASE = 30;

    // One phase = one walk through the diagram, from a restart until the next restart.
    public PhaseSummary run(PhaseState start, MatchContext context) {
        MatchTeam startingTeam = context.attackingTeam();
        PhaseState current = start;
        int totalSeconds = 0;

        for (int step = 0; step < MAX_STATES_PER_PHASE; step++) {
            MatchTeam teamOnTheBall = context.attackingTeam();
            StateResult result = current.execute(context);

            context.passTime(result.seconds());
            context.addPossessionTime(teamOnTheBall, result.seconds());
            totalSeconds += result.seconds();

            if (result.next().isRestart()) {
                return new PhaseSummary(start, result.next(), totalSeconds, startingTeam);
            }
            current = result.next();
        }
        throw new IllegalStateException("Phase did not end after " + MAX_STATES_PER_PHASE + " states (last: " + current + ")");
    }
}
