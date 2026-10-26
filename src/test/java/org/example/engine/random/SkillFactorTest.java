package org.example.engine.random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillFactorTest {

    @Test
    void equalPlayersChangeNothing() {
        assertEquals(1.0, SkillFactor.of(80, 80), 1e-9);
        assertEquals(1.0, SkillFactor.of(80, 80, 9), 1e-9);
    }

    @Test
    void theRatioIsSquared() {
        assertEquals(4.0, SkillFactor.of(90, 45), 1e-9);
        assertEquals(0.25, SkillFactor.of(45, 90), 1e-9);
    }

    @Test
    void aStrongerExponentMakesTheSameGapCountMore() {
        double players = SkillFactor.of(90, 86);
        double teams = SkillFactor.of(90, 86, 9);
        assertTrue(teams > players);
        assertEquals(Math.pow(90.0 / 86, 9), teams, 1e-9);
    }

    @Test
    void attributesMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> SkillFactor.of(0, 80));
        assertThrows(IllegalArgumentException.class, () -> SkillFactor.of(80, -1, 9));
    }
}
