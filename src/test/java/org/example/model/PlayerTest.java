package org.example.model;

import org.example.TestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTest {

    @Test
    void fiveYellowCardsGiveAOneMatchBan() {
        Player player = TestData.player();

        for (int i = 0; i < 4; i++) {
            assertFalse(player.addYellowCard());
        }
        assertTrue(player.isAvailable());

        assertTrue(player.addYellowCard());                    // the fifth one completes the ban
        assertEquals(PlayerStatus.SUSPENDED, player.getStatus());
        assertEquals(0, player.getYellowCardsAccumulated());   // the counter starts again
    }

    @Test
    void aBanIsServedByMissingMatches() {
        Player player = TestData.player();
        player.suspend(2);

        player.missMatch();
        assertEquals(PlayerStatus.SUSPENDED, player.getStatus());
        player.missMatch();
        assertTrue(player.isAvailable());
    }

    @Test
    void anInjuryCountsBeforeABan() {
        Player player = TestData.player();
        player.suspend(1);
        player.injure(1);

        assertEquals(PlayerStatus.INJURED, player.getStatus());
        player.missMatch();                                     // one missed match serves both
        assertTrue(player.isAvailable());
    }

    @Test
    void aSecondInjuryDoesNotAddUp() {
        Player player = TestData.player();
        player.injure(2);
        player.injure(1);
        assertEquals(2, player.getMatchesInjured());
    }

    @Test
    void conditionStaysBetweenZeroAndOneHundred() {
        Player player = TestData.player();
        assertEquals(Player.MAX_CONDITION, player.getCondition());

        player.recoverCondition(30);
        assertEquals(100, player.getCondition());

        player.reduceCondition(130);
        assertEquals(0, player.getCondition());

        player.recoverCondition(20);
        assertEquals(20, player.getCondition());
    }

    @Test
    void banAndInjuryMustLastAtLeastOneMatch() {
        Player player = TestData.player();
        assertThrows(IllegalArgumentException.class, () -> player.suspend(0));
        assertThrows(IllegalArgumentException.class, () -> player.injure(-1));
    }

    @Test
    void restoredStateComesBackExactly() {
        Player player = TestData.player();
        player.restoreState(64, 3, 1, 0);

        assertEquals(64, player.getCondition());
        assertEquals(3, player.getYellowCardsAccumulated());
        assertEquals(PlayerStatus.SUSPENDED, player.getStatus());
    }

    @Test
    void invalidSavedStateIsRejected() {
        Player player = TestData.player();
        assertThrows(IllegalArgumentException.class, () -> player.restoreState(101, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> player.restoreState(80, -1, 0, 0));
    }

    @Test
    void shirtNumberMustBeValid() {
        assertThrows(IllegalArgumentException.class, () -> new Player(1, "A", "B", "C", 25, 100,
                DetailedPosition.ST, PreferredFoot.LEFT, TestData.attributes(80)));
    }
}
