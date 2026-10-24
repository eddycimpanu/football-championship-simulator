package org.example;

import org.example.engine.ShotOutcome;
import org.example.engine.random.RandomProvider;
import org.example.engine.random.RouletteWheel;
import org.example.engine.random.SkillFactor;
import org.example.model.Player;
import org.example.model.Team;
import org.example.persistence.ConnectionProvider;
import org.example.persistence.DatabaseConfig;
import org.example.persistence.LeagueLoader;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final double GOAL_BASE = 15;
    private static final double SAVE_BASE = 35;
    private static final double MISS_BASE = 50;

    public static void main(String[] args) {
        ConnectionProvider connectionProvider = new ConnectionProvider(DatabaseConfig.load());
        List<Team> teams = new LeagueLoader(connectionProvider).loadTeams();

        RandomProvider random = new RandomProvider(42);

        Player messi = findPlayer(teams, "Messi");
        Player nicolita = findPlayer(teams, "Nicoliță");
        Player buffon = findPlayer(teams, "Buffon");

        testShot(messi, buffon, random);
        testShot(nicolita, buffon, random);
        testSameSeed();
        testValidation();
    }

    private static RouletteWheel<ShotOutcome> shotWheel(Player shooter, Player keeper) {
        double skill = SkillFactor.of(shooter.getAttributes().shooting(), keeper.getAttributes().reflexes());
        return new RouletteWheel<ShotOutcome>()
                .addFavourable(ShotOutcome.GOAL, GOAL_BASE, skill)
                .addOption(ShotOutcome.SAVE, SAVE_BASE)
                .addUnfavourable(ShotOutcome.MISS, MISS_BASE, skill);
    }

    private static void testShot(Player shooter, Player keeper, RandomProvider random) {
        System.out.println();
        System.out.println("=== " + shooter.getFullName() + " (SHOOTING " + shooter.getAttributes().shooting()
                + ") shoots at " + keeper.getFullName() + " (REFLEXES " + keeper.getAttributes().reflexes() + ") ===");

        RouletteWheel<ShotOutcome> wheel = shotWheel(shooter, keeper);
        System.out.print(wheel.describe());

        double roll = random.nextDouble();
        System.out.printf("One spin: roll %.3f -> %s%n", roll, wheel.pick(roll));

        int shots = 100_000;
        Map<ShotOutcome, Integer> counts = new EnumMap<>(ShotOutcome.class);
        for (int i = 0; i < shots; i++) {
            counts.merge(wheel.spin(random), 1, Integer::sum);
        }
        System.out.println("After " + shots + " shots:");
        for (ShotOutcome outcome : ShotOutcome.values()) {
            double actual = 100.0 * counts.getOrDefault(outcome, 0) / shots;
            double expected = 100.0 * wheel.probabilityOf(outcome);
            System.out.printf("  %-6s %5.1f%%  (expected %5.1f%%)%n", outcome, actual, expected);
        }
    }

    private static void testSameSeed() {
        System.out.println();
        System.out.println("=== Same seed, same season ===");
        RandomProvider first = new RandomProvider(7);
        RandomProvider second = new RandomProvider(7);
        boolean same = true;
        for (int i = 0; i < 1000; i++) {
            if (first.nextDouble() != second.nextDouble()) {
                same = false;
            }
        }
        System.out.println("1000 numbers identical: " + same);
    }

    private static void testValidation() {
        System.out.println();
        System.out.println("=== Validation (every line should be an expected error) ===");

        try {
            new RouletteWheel<String>().addOption("GOAL", -5);
            System.out.println("FAILED: negative weight was accepted");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        try {
            new RouletteWheel<String>().addOption("GOAL", 0).pick(0.5);
            System.out.println("FAILED: empty wheel was spun");
        } catch (IllegalStateException e) {
            System.out.println("Expected error: " + e.getMessage());
        }
    }

    private static Player findPlayer(List<Team> teams, String lastName) {
        return teams.stream()
                .flatMap(team -> team.getSquad().stream())
                .filter(player -> player.getLastName().equals(lastName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + lastName));
    }
}