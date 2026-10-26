package org.example.betting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionTest {

    @Test
    void resultMarket() {
        assertTrue(Selection.HOME.wins(2, 1));
        assertTrue(Selection.DRAW.wins(1, 1));
        assertTrue(Selection.AWAY.wins(0, 3));
        assertFalse(Selection.HOME.wins(1, 1));
    }

    @Test
    void overUnderTwoAndAHalfGoals() {
        assertTrue(Selection.OVER_2_5.wins(2, 1));
        assertFalse(Selection.OVER_2_5.wins(1, 1));
        assertTrue(Selection.UNDER_2_5.wins(2, 0));
    }

    @Test
    void bothTeamsScore() {
        assertTrue(Selection.BOTH_SCORE.wins(1, 1));
        assertTrue(Selection.NOT_BOTH_SCORE.wins(3, 0));
        assertFalse(Selection.BOTH_SCORE.wins(0, 0));
    }

    @Test
    void exactlyOneSelectionOfEachMarketWins() {
        for (int home = 0; home <= 5; home++) {
            for (int away = 0; away <= 5; away++) {
                for (Selection.Market market : Selection.Market.values()) {
                    int winners = 0;
                    for (Selection selection : Selection.values()) {
                        if (selection.getMarket() == market && selection.wins(home, away)) {
                            winners++;
                        }
                    }
                    assertEquals(1, winners, market + " at " + home + "-" + away);
                }
            }
        }
    }
}
