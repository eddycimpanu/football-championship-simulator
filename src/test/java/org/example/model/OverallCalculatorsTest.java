package org.example.model;

import org.example.TestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverallCalculatorsTest {

    @Test
    void everyPositionHasACalculator() {
        for (DetailedPosition position : DetailedPosition.values()) {
            assertNotNull(OverallCalculators.forPosition(position), position.name());
        }
    }

    @Test
    void betterAttributesGiveAHigherOverall() {
        for (DetailedPosition position : DetailedPosition.values()) {
            OverallCalculator calculator = OverallCalculators.forPosition(position);
            assertTrue(calculator.calculate(TestData.attributes(85)) > calculator.calculate(TestData.attributes(70)),
                    position.name());
        }
    }

    @Test
    void overallNeverGoesAboveNinetyNine() {
        for (DetailedPosition position : DetailedPosition.values()) {
            assertTrue(OverallCalculators.forPosition(position).calculate(TestData.attributes(99)) <= 99, position.name());
        }
    }

    @Test
    void aStrikerIsRatedOnShootingNotOnDefending() {
        PlayerAttributes finisher = new PlayerAttributes(80, 95, 75, 80, 30, 75, 75, 10, 10, 75, 60);
        PlayerAttributes stopper = new PlayerAttributes(80, 30, 75, 80, 95, 75, 75, 10, 10, 75, 60);
        OverallCalculator striker = OverallCalculators.forPosition(DetailedPosition.ST);

        assertTrue(striker.calculate(finisher) > striker.calculate(stopper));
    }
}
