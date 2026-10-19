package org.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Team {

    public static final int MAX_SQUAD_SIZE = 30;

    private final int id;
    private final String name;
    private final String shortName;
    private final String city;
    private final String stadium;
    private final Coach coach;
    private final List<Player> squad = new ArrayList<>();

    public Team(int id, String name, String shortName, String city, String stadium, Coach coach) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name is required");
        }
        if (shortName == null || !shortName.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Short name must have exactly 3 letters, got " + shortName);
        }
        if (coach == null) {
            throw new IllegalArgumentException("Team " + name + " must have a coach");
        }
        this.id = id;
        this.name = name;
        this.shortName = shortName.toUpperCase();
        this.city = city;
        this.stadium = stadium;
        this.coach = coach;
    }

    public void addPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (squad.size() >= MAX_SQUAD_SIZE) {
            throw new IllegalStateException(name + " already has " + MAX_SQUAD_SIZE + " players");
        }
        if (findByShirtNumber(player.getShirtNumber()).isPresent()) {
            throw new IllegalArgumentException(
                    "Shirt number " + player.getShirtNumber() + " is already used in " + name);
        }
        squad.add(player);
    }

    public boolean removePlayer(Player player) {
        return squad.remove(player);
    }

    public Optional<Player> findByShirtNumber(int shirtNumber) {
        return squad.stream()
                .filter(player -> player.getShirtNumber() == shirtNumber)
                .findFirst();
    }

    public List<Player> getSquad() {
        return Collections.unmodifiableList(squad);
    }

    public List<Player> getAvailablePlayers() {
        return squad.stream()
                .filter(Player::isAvailable)
                .toList();
    }

    public List<Player> getPlayersByFieldRole(FieldRole fieldRole) {
        return squad.stream()
                .filter(player -> player.getFieldRole() == fieldRole)
                .toList();
    }

    public List<Player> getAvailablePlayersByFieldRole(FieldRole fieldRole) {
        return squad.stream()
                .filter(Player::isAvailable)
                .filter(player -> player.getFieldRole() == fieldRole)
                .toList();
    }

    public double getAverageOverall() {
        return squad.stream()
                .mapToInt(Player::getOverall)
                .average()
                .orElse(0);
    }

    public int getSquadSize() { return squad.size(); }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getCity() { return city; }
    public String getStadium() { return stadium; }
    public Coach getCoach() { return coach; }

    @Override
    public String toString() {
        return name + " (" + shortName + ")";
    }
}