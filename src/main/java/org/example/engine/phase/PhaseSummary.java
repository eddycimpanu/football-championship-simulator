package org.example.engine.phase;

import org.example.engine.MatchTeam;

public record PhaseSummary(PhaseState start, PhaseState nextStart, int durationSeconds, MatchTeam startingTeam) {
}
