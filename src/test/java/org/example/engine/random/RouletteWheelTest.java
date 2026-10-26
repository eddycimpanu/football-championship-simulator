package org.example.engine.random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouletteWheelTest {

    private enum Result { GOAL, SAVE, MISS }

    @Test
    void probabilityIsTheWeightDividedByTheTotal() {
        RouletteWheel<Result> wheel = new RouletteWheel<Result>()
                .addOption(Result.GOAL, 10)
                .addOption(Result.SAVE, 30)
                .addOption(Result.MISS, 60);

        assertEquals(0.10, wheel.probabilityOf(Result.GOAL), 1e-9);
        assertEquals(0.30, wheel.probabilityOf(Result.SAVE), 1e-9);
        assertEquals(0.60, wheel.probabilityOf(Result.MISS), 1e-9);
    }

    @Test
    void skillFactorGrowsFavourableAndShrinksUnfavourableSlices() {
        RouletteWheel<Result> wheel = new RouletteWheel<Result>()
                .addFavourable(Result.GOAL, 10, 2)      // 10 x 2 = 20
                .addOption(Result.SAVE, 30)
                .addUnfavourable(Result.MISS, 60, 2);   // 60 / 2 = 30

        assertEquals(20.0 / 80, wheel.probabilityOf(Result.GOAL), 1e-9);
        assertEquals(30.0 / 80, wheel.probabilityOf(Result.MISS), 1e-9);
    }

    @Test
    void theRollPicksTheSliceItLandsIn() {
        RouletteWheel<Result> wheel = new RouletteWheel<Result>()
                .addOption(Result.GOAL, 10)    // 0.0 - 0.1
                .addOption(Result.SAVE, 30)    // 0.1 - 0.4
                .addOption(Result.MISS, 60);   // 0.4 - 1.0

        assertEquals(Result.GOAL, wheel.pick(0.0));
        assertEquals(Result.GOAL, wheel.pick(0.0999));
        assertEquals(Result.SAVE, wheel.pick(0.1));
        assertEquals(Result.SAVE, wheel.pick(0.39));
        assertEquals(Result.MISS, wheel.pick(0.4));
        assertEquals(Result.MISS, wheel.pick(0.9999));
    }

    @Test
    void optionsWithZeroWeightAreNeverPicked() {
        RouletteWheel<Result> wheel = new RouletteWheel<Result>()
                .addOption(Result.GOAL, 0)
                .addOption(Result.MISS, 5);

        assertEquals(0, wheel.probabilityOf(Result.GOAL), 1e-9);
        assertEquals(Result.MISS, wheel.pick(0.0));
    }

    @Test
    void spinsFollowTheProbabilities() {
        RouletteWheel<Result> wheel = new RouletteWheel<Result>()
                .addOption(Result.GOAL, 10)
                .addOption(Result.SAVE, 30)
                .addOption(Result.MISS, 60);
        RandomProvider random = new RandomProvider(42);

        int spins = 100_000;
        int goals = 0;
        for (int i = 0; i < spins; i++) {
            if (wheel.spin(random) == Result.GOAL) {
                goals++;
            }
        }
        assertEquals(0.10, (double) goals / spins, 0.01);
    }

    @Test
    void theSameSeedGivesTheSameSpins() {
        RouletteWheel<Result> wheel = new RouletteWheel<Result>()
                .addOption(Result.GOAL, 1)
                .addOption(Result.SAVE, 1)
                .addOption(Result.MISS, 1);
        RandomProvider first = new RandomProvider(7);
        RandomProvider second = new RandomProvider(7);

        for (int i = 0; i < 100; i++) {
            assertEquals(wheel.spin(first), wheel.spin(second));
        }
    }

    @Test
    void negativeWeightIsRejected() {
        RouletteWheel<Result> wheel = new RouletteWheel<>();
        assertThrows(IllegalArgumentException.class, () -> wheel.addOption(Result.GOAL, -1));
    }

    @Test
    void anEmptyWheelCannotBeSpun() {
        RouletteWheel<Result> wheel = new RouletteWheel<Result>().addOption(Result.GOAL, 0);
        assertThrows(IllegalStateException.class, () -> wheel.pick(0.5));
    }
}
