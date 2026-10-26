package org.example.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreTest {

    @Test
    void aGoalGivesANewScoreAndLeavesTheOldOneUnchanged() {
        Score start = Score.ZERO;
        Score afterHomeGoal = start.homeScores();
        Score afterAwayGoal = afterHomeGoal.awayScores();

        assertEquals(new Score(0, 0), start);
        assertEquals(new Score(1, 0), afterHomeGoal);
        assertEquals(new Score(1, 1), afterAwayGoal);
    }

    @Test
    void isShownHomeFirst() {
        assertEquals("2-1", new Score(2, 1).toString());
    }
}
