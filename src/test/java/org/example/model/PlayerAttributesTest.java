package org.example.model;

import org.example.TestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerAttributesTest {

    @Test
    void attributesMustBeBetweenOneAndNinetyNine() {
        assertThrows(IllegalArgumentException.class, () -> TestData.attributes(0));
        assertThrows(IllegalArgumentException.class, () -> TestData.attributes(100));
    }

    @Test
    void fatigueLowersTheSkillsButNotStaminaOrAggression() {
        PlayerAttributes fresh = TestData.attributes(80);
        PlayerAttributes tired = fresh.withFatigue(0.9);

        assertEquals(72, tired.shooting());
        assertEquals(72, tired.pace());
        assertEquals(80, tired.stamina());
        assertEquals(80, tired.aggression());
    }

    @Test
    void aFreshPlayerIsNotChanged() {
        PlayerAttributes fresh = TestData.attributes(80);
        assertSame(fresh, fresh.withFatigue(1.0));
    }

    @Test
    void getReadsTheRightAttribute() {
        PlayerAttributes attributes = new PlayerAttributes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        assertEquals(1, attributes.get(Attribute.PACE));
        assertEquals(7, attributes.get(Attribute.HEADING));
        assertEquals(11, attributes.get(Attribute.AGGRESSION));
    }
}
