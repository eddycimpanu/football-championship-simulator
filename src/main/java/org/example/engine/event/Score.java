package org.example.engine;

public record Score(int home, int away) {

    public static final Score ZERO = new Score(0, 0);

    public Score {
        if (home < 0 || away < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
    }

    public Score homeScores() {
        return new Score(home + 1, away);
    }

    public Score awayScores() {
        return new Score(home, away + 1);
    }

    @Override
    public String toString() {
        return home + "-" + away;
    }
}