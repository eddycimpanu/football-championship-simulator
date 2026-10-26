package org.example.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        this.id = id; this.name = name; this.shortName = shortName.toUpperCase();
        this.city = city; this.stadium = stadium; this.coach = coach;
    }
    public void addPlayer(Player player) {
        if (squad.size() >= MAX_SQUAD_SIZE) throw new IllegalStateException("full");
        squad.add(player);
    }
    public List<Player> getSquad() { return Collections.unmodifiableList(squad); }
    public List<Player> getAvailablePlayers() { return squad.stream().filter(Player::isAvailable).toList(); }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getCity() { return city; }
    public String getStadium() { return stadium; }
    public Coach getCoach() { return coach; }
    @Override public String toString() { return name + " (" + shortName + ")"; }
}
