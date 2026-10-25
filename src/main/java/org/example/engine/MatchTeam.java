package org.example.engine;

import org.example.config.EngineConfig;
import org.example.model.Attribute;
import org.example.model.FieldRole;
import org.example.model.Lineup;
import org.example.model.Player;
import org.example.model.PlayerAttributes;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.ToIntFunction;

public class MatchTeam {

    private final Team team;
    private final boolean home;
    private final Lineup lineup;
    private final List<Player> onPitch;
    private final List<Player> bench;

    private final Map<Player, Double> energy = new HashMap<>();
    private final Map<Player, Double> energyUsed = new HashMap<>();

    private final List<Player> unavailableAtKickoff;
    private final Map<Player, Integer> yellowCards = new HashMap<>();
    private final Set<Player> sentOff = new HashSet<>();
    private boolean coachHasYellow;
    private boolean coachSentOff;

    private MatchTeam(Team team, boolean home, Lineup lineup) {
        this.team = team;
        this.home = home;
        this.lineup = lineup;
        this.onPitch = new ArrayList<>(lineup.startingEleven());
        this.bench = new ArrayList<>(lineup.bench());
        for (Player player : onPitch) {
            energy.put(player, (double) player.getCondition());
        }
        for (Player player : bench) {
            energy.put(player, (double) player.getCondition());
        }
        this.unavailableAtKickoff = team.getSquad().stream().filter(p -> !p.isAvailable()).toList();
    }

    // The coach picks the starting eleven and the bench.
    public static MatchTeam prepare(Team team, boolean home) {
        return new MatchTeam(team, home, team.getCoach().selectStartingEleven(team));
    }

    // ----- Energy and fatigue -----

    // Energy drops every minute for the players on the pitch; high STAMINA drops slower.
    public void drainEnergy(int seconds) {
        for (Player player : onPitch) {
            double stamina = player.getAttributes().stamina();
            double perMinute = EngineConfig.ENERGY_DROP_PER_MINUTE * (2 - stamina / EngineConfig.REFERENCE_ATTRIBUTE);
            if (player.getFieldRole() == FieldRole.GOALKEEPER) {
                perMinute *= EngineConfig.GOALKEEPER_ENERGY_FACTOR;
            }
            double drop = Math.max(0, perMinute) * seconds / 60.0;
            energy.merge(player, -drop, (current, change) -> Math.max(0, current + change));
            energyUsed.merge(player, drop, Double::sum);
        }
    }

    public void recoverEnergy(double amount) {
        for (Player player : onPitch) {
            energy.merge(player, amount, (current, change) -> Math.min(Player.MAX_CONDITION, current + change));
        }
    }

    public double getEnergy(Player player) {
        return energy.getOrDefault(player, (double) player.getCondition());
    }

    // The attributes a player really has right now: a tired player is a bit weaker.
    public PlayerAttributes attributesOf(Player player) {
        double missingEnergy = 1 - getEnergy(player) / Player.MAX_CONDITION;
        return player.getAttributes().withFatigue(1 - EngineConfig.FATIGUE_EFFECT * missingEnergy);
    }

    // After the final whistle: everyone who played loses condition for the energy he used.
    public void applyConditionLoss() {
        energyUsed.forEach((player, used) ->
                player.reduceCondition((int) Math.round(used * EngineConfig.CONDITION_COST_PER_ENERGY)));
    }

    // ----- Discipline -----

    // Returns true when this is his second yellow card: he is sent off.
    public boolean giveYellowCard(Player player) {
        int cards = yellowCards.merge(player, 1, Integer::sum);
        if (cards >= 2) {
            sendOff(player);
            return true;
        }
        return false;
    }

    public boolean hasYellowCard(Player player) {
        return yellowCards.getOrDefault(player, 0) > 0;
    }

    // A player who is sent off leaves the pitch and cannot be replaced.
    public void sendOff(Player player) {
        if (onPitch.remove(player)) {
            sentOff.add(player);
        }
    }

    public boolean giveCoachYellowCard() {
        if (coachHasYellow) {
            sendOffCoach();
            return true;
        }
        coachHasYellow = true;
        return false;
    }

    public void sendOffCoach() {
        coachSentOff = true;
    }

    // 11 players = 1.0, 10 players = 0.9, 9 players = 0.81 ...
    public double manpowerFactor() {
        return Math.pow(EngineConfig.RED_CARD_FACTOR, sentOff.size());
    }

    // After the final whistle: bans served, yellow cards counted, red cards punished.
    public void applyDisciplineAfterMatch() {
        for (Player player : unavailableAtKickoff) {
            player.missMatch();
        }
        yellowCards.forEach((player, cards) -> {
            if (cards == 1 && !sentOff.contains(player)) {
                player.addYellowCard();
            }
        });
        for (Player player : sentOff) {
            player.suspend(1);
        }
    }

    // ----- Ratings, always from the players on the pitch right now -----

    public double getAttackRating() {
        return average(playersIn(FieldRole.MIDFIELDER, FieldRole.FORWARD), p -> {
            PlayerAttributes a = attributesOf(p);
            return (a.passing() + a.dribbling() + a.pace()) / 3;
        });
    }

    public double getDefenceRating() {
        return average(playersIn(FieldRole.DEFENDER, FieldRole.MIDFIELDER), p -> {
            PlayerAttributes a = attributesOf(p);
            return (a.defending() + a.physical() + a.pace()) / 3;
        });
    }

    public double averageOf(Attribute attribute, FieldRole... roles) {
        return average(playersIn(roles), p -> attributesOf(p).get(attribute));
    }

    public double averageOfTop(Attribute attribute, int count) {
        return getOutfieldPlayers().stream()
                .mapToInt(p -> attributesOf(p).get(attribute))
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

    public boolean has(Player player) {
        return energy.containsKey(player);
    }

    public boolean isOnPitch(Player player) {
        return onPitch.contains(player);
    }

    public int playersOnPitch() {
        return onPitch.size();
    }

    public List<Player> getSentOff() {
        return List.copyOf(sentOff);
    }

    public boolean isCoachSentOff() {
        return coachSentOff;
    }

    // If the goalkeeper was sent off, the outfield player with the best hands goes in goal.
    public Player getGoalkeeper() {
        return onPitch.stream()
                .filter(p -> p.getFieldRole() == FieldRole.GOALKEEPER)
                .findFirst()
                .orElseGet(() -> onPitch.stream()
                        .max(Comparator.comparingInt(p -> p.getAttributes().reflexes() + p.getAttributes().handling()))
                        .orElseThrow());
    }

    public List<Player> getOutfieldPlayers() {
        return onPitch.stream().filter(p -> p.getFieldRole() != FieldRole.GOALKEEPER).toList();
    }

    public List<Player> getOnPitch() { return Collections.unmodifiableList(onPitch); }
    public List<Player> getStartingEleven() { return lineup.startingEleven(); }
    public List<Player> getBench() { return Collections.unmodifiableList(bench); }
    public Lineup getLineup() { return lineup; }
    public Team getTeam() { return team; }
    public String getName() { return team.getName(); }
    public boolean isHome() { return home; }

    @Override
    public String toString() {
        return team.getName();
    }
}
