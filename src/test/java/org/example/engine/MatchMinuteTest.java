package org.example.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchMinuteTest {

    @Test
    void stoppageTimeIsShownAfterThePlus() {
        assertEquals("45+3'", new MatchMinute(45, 3).toString());
        assertEquals("90+5'", new MatchMinute(90, 5).toString());
        assertEquals("27'", MatchMinute.of(27).toString());
    }

    @Test
    void stoppageTimeComesAfterTheMinuteItBelongsTo() {
        assertTrue(new MatchMinute(45, 2).compareTo(MatchMinute.of(45)) > 0);
        assertTrue(new MatchMinute(45, 4).compareTo(MatchMinute.of(46)) < 0);
    }

    @Test
    void stoppageTimeOnlyExistsAtTheEndOfEachHalf() {
        assertThrows(IllegalArgumentException.class, () -> new MatchMinute(30, 2));
    }

    @Test
    void minutesOutsideTheMatchAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MatchMinute.of(0));
        assertThrows(IllegalArgumentException.class, () -> MatchMinute.of(91));
        assertThrows(IllegalArgumentException.class, () -> new MatchMinute(90, -1));
    }
}
