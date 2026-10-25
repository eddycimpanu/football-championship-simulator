package org.example.engine;

import org.example.model.Attribute;
import org.example.model.DetailedPosition;
import org.example.model.FieldRole;
import org.example.model.Player;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

public class MatchTeam {

    private static final int BENCH_SIZE = 9;
    private static final int SAME_ROLE_PENALTY = 2;
    private static final int OTHER_ROLE_PENALTY = 10;

    private final Team team;
    private final boolean home;
    private final List<Player> startingEleven;
    private final List<Player> onPitch;
    private final List<Player> bench;

    private MatchTeam(Team team, boolean home, List<Player> startingEleven, List<Player> bench) {
        this.team = team;
        this.home = home;
        this.startingEleven = List.copyOf(startingEleven);
        this.onPitch = new ArrayList<>(startingEleven);
        this.bench = new ArrayList<>(bench);
    }

    // Temporary line-up: the best available player for every slot of the coach's formation.
    // Stage 17 replaces this with the coach's own choice (fitness, rotation).
    public static MatchTeam prepare(Team team, boolean home) {
        List<Player> available = new ArrayList<>(team.getAvailablePlayers());
        List<Player> eleven = new ArrayList<>();

        for (DetailedPosition slot : team.getCoach().getPreferredFormation().getSlots()) {
            Player chosen = available.stream()
                    .filter(player -> (slot == DetailedPosition.GK) == (player.getFieldRole() == FieldRole.GOALKEEPER))
                    .max(Comparator.comparingInt(player -> slotScore(player, slot)))
                    .orElseThrow(() -> new IllegalStateException(team.getName() + " cannot fill " + slot));
            eleven.add(chosen);
            available.remove(chosen);
        }

        available.sort(Comparator.comparingInt(Player::getOverall).reversed());
        List<Player> bench = new ArrayList<>();
        available.stream().filter(p -> p.getFieldRole() == FieldRole.GOALKEEPER).findFirst().ifPresent(bench::add);
        for (Player player : available) {
            if (bench.size() >= BENCH_SIZE) {
                break;
            }
            if (!bench.contains(player)) {
                bench.add(player);
            }
        }
        return new MatchTeam(team, home, eleven, bench);
    }

    private static int slotScore(Player player, DetailedPosition slot) {
        int penalty = player.getPosition() == slot ? 0
                : player.getFieldRole() == slot.getFieldRole() ? SAME_ROLE_PENALTY : OTHER_ROLE_PENALTY;
        return player.getOverallAs(slot) - penalty;
    }

    // ----- Ratings, always from the players on the pitch right now -----

    public double getAttackRating() {
        return average(playersIn(FieldRole.MIDFIELDER, FieldRole.FORWARD),
                p -> (p.getAttributes().passing() + p.getAttributes().dribbling() + p.getAttributes().pace()) / 3);
    }

    public double getDefenceRating() {
        return average(playersIn(FieldRole.DEFENDER, FieldRole.MIDFIELDER),
                p -> (p.getAttributes().defending() + p.getAttributes().physical() + p.getAttributes().pace()) / 3);
    }

    public double averageOf(Attribute attribute, FieldRole... roles) {
        return average(playersIn(roles), p -> p.getAttributes().get(attribute));
    }

    public double averageOfTop(Attribute attribute, int count) {
        return getOutfieldPlayers().stream()
                .mapToInt(p -> p.getAttributes().get(attribute))
                .boxed()
                .sorted(Comparator.reverseOrder())
                .limit(count)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(1);
    }

    private List<Player> playersIn(FieldRole... roles) {
        List<FieldRole> wanted = List.of(roles);
        return onPitch.stream().filter(p -> wanted.contains(p.getFieldRole())).toList();
    }

    private static double average(List<Player> players, ToIntFunction<Player> value) {
        return players.stream().mapToInt(value).average().orElse(1);
    }

    // ----- Players -----

    public Player getGoalkeeper() {
        return onPitch.stream()
                .filter(p -> p.getFieldRole() == FieldRole.GOALKEEPER)
                .findFirst()
                .orElseGet(() -> onPitch.get(0));
    }

    public List<Player> getOutfieldPlayers() {
        return onPitch.stream().filter(p -> p.getFieldRole() != FieldRole.GOALKEEPER).toList();
    }

    public List<Player> getOnPitch() { return Collections.unmodifiableList(onPitch); }
    public List<Player> getStartingEleven() { return startingEleven; }
    public List<Player> getBench() { return Collections.unmodifiableList(bench); }
    public Team getTeam() { return team; }
    public String getName() { return team.getName(); }
    public boolean isHome() { return home; }

    @Override
    public String toString() {
        return team.getName();
    }
}
