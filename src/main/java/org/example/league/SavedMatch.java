package org.example.league;

import org.example.engine.Score;

// One row of the schedule as it is stored in the database. The score is null until the match is played.
public record SavedMatch(int id, int round, int homeTeamId, int awayTeamId, int refereeId, Score score) {

    public boolean isPlayed() {
        return score != null;
    }
}
