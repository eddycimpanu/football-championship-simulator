package org.example;

import org.example.engine.MatchClock;
import org.example.engine.MatchMinute;
import org.example.engine.MatchPeriod;
import org.example.engine.random.RandomProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        testMinuteFormat();
        testFakeMatch();
    }

    private static void testMinuteFormat() {
        System.out.println("=== Minute format ===");
        System.out.println(MatchMinute.of(1) + "  " + MatchMinute.of(34) + "  "
                + new MatchMinute(45, 3) + "  " + MatchMinute.of(48) + "  " + new MatchMinute(90, 4));

        List<MatchMinute> minutes = new ArrayList<>(List.of(
                MatchMinute.of(46), new MatchMinute(45, 3), MatchMinute.of(45),
                new MatchMinute(90, 1), MatchMinute.of(90)));
        Collections.sort(minutes);
        System.out.println("Sorted: " + minutes + "  (expected [45', 45+3', 46', 90', 90+1'])");

        try {
            new MatchMinute(60, 2);
            System.out.println("FAILED: 60+2' was accepted");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }
    }

    private static void testFakeMatch() {
        System.out.println();
        System.out.println("=== Fake match (random events, no teams yet) ===");
        RandomProvider random = new RandomProvider(42);
        MatchClock clock = new MatchClock();

        System.out.println("1' KICK-OFF");
        playPeriod(clock, random);
        System.out.println(clock.getMinute() + " HALF-TIME");

        clock.startSecondHalf();
        System.out.println(clock.getMinute() + " SECOND HALF STARTS");
        playPeriod(clock, random);
        System.out.println(clock.getMinute() + " FULL-TIME");
    }

    private static void playPeriod(MatchClock clock, RandomProvider random) {
        int phases = 0;
        while (!clock.isPeriodOver()) {
            clock.advance(random.nextInt(10, 90));
            phases++;

            if (random.chance(0.03)) {
                clock.recordGoal();
                System.out.println(clock.getMinute() + " GOAL");
            }
            if (random.chance(0.04)) {
                clock.recordCard();
                System.out.println(clock.getMinute() + " YELLOW CARD");
            }
            if (clock.getPeriod() == MatchPeriod.SECOND_HALF && random.chance(0.05)) {
                clock.recordSubstitution();
                System.out.println(clock.getMinute() + " SUBSTITUTION");
            }
            if (clock.isRegulationTimeOver() && !clock.isStoppageTimeAnnounced()) {
                int added = clock.announceStoppageTime(random);
                System.out.println("    +" + added + " minutes of stoppage time");
            }
        }
        System.out.println("    (" + phases + " phases in the " + clock.getPeriod().toString().toLowerCase() + ")");
    }
}