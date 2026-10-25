package org.example.model;

import org.example.config.EngineConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Coach extends Person {

    private static final int BENCH_SIZE = 9;
    private static final int SAME_ROLE_PENALTY = 2;
    private static final int OTHER_ROLE_PENALTY = 10;

    private final Formation preferredFormation;

    public Coach(int id, String firstName, String lastName, String nationality, int age,
                 Formation preferredFormation) {
        super(id, firstName, lastName, nationality, age);
        if (preferredFormation == null) {
            throw new IllegalArgumentException("Preferred formation is required");
        }
        this.preferredFormation = preferredFormation;
    }

    @Override
    public String getRole() {
        return "Coach";
    }

    // For every slot of the formation: the available player with the best score.
    // Score = how good he is on that position, lowered when he is tired or out of position.
    public Lineup selectStartingEleven(Team team) {
        List<Player> candidates = new ArrayList<>(team.getAvailablePlayers());
        List<Player> rested = candidates.stream()
                .filter(p -> p.getCondition() >= EngineConfig.MIN_CONDITION_TO_START)
                .toList();
        List<Player> eleven = new ArrayList<>();

        for (DetailedPosition slot : preferredFormation.getSlots()) {
            Player chosen = bestFor(slot, rested, eleven)
                    .orElseGet(() -> bestFor(slot, candidates, eleven)
                            .orElseThrow(() -> new IllegalStateException(team.getName() + " cannot fill " + slot)));
            eleven.add(chosen);
        }

        List<Player> bench = selectBench(candidates, eleven);
        return new Lineup(preferredFormation, eleven, bench);
    }

    private Optional<Player> bestFor(DetailedPosition slot, List<Player> pool, List<Player> taken) {
        boolean goalkeeperSlot = slot == DetailedPosition.GK;
        return pool.stream()
                .filter(p -> !taken.contains(p))
                .filter(p -> goalkeeperSlot == (p.getFieldRole() == FieldRole.GOALKEEPER))
                .max(Comparator.comparingDouble(p -> selectionScore(p, slot)));
    }

    private double selectionScore(Player player, DetailedPosition slot) {
        int penalty = player.getPosition() == slot ? 0
                : player.getFieldRole() == slot.getFieldRole() ? SAME_ROLE_PENALTY : OTHER_ROLE_PENALTY;
        double fitness = 1 - EngineConfig.FITNESS_WEIGHT_IN_SELECTION * (1 - player.getCondition() / 100.0);
        return (player.getOverallAs(slot) - penalty) * fitness;
    }

    // The best players left, always with a reserve goalkeeper.
    private List<Player> selectBench(List<Player> candidates, List<Player> eleven) {
        List<Player> left = new ArrayList<>(candidates);
        left.removeAll(eleven);
        left.sort(Comparator.comparingInt(Player::getOverall).reversed());

        List<Player> bench = new ArrayList<>();
        left.stream().filter(p -> p.getFieldRole() == FieldRole.GOALKEEPER).findFirst().ifPresent(bench::add);
        for (Player player : left) {
            if (bench.size() >= BENCH_SIZE) {
                break;
            }
            if (!bench.contains(player)) {
                bench.add(player);
            }
        }
        return bench;
    }

    public Formation getPreferredFormation() {
        return preferredFormation;
    }

    @Override
    public String toString() {
        return getFullName() + " (Coach, " + preferredFormation + ")";
    }
}
