package org.example;

import org.example.model.Coach;
import org.example.model.DetailedPosition;
import org.example.model.Formation;
import org.example.model.Player;
import org.example.model.PlayerAttributes;
import org.example.model.PreferredFoot;
import org.example.model.Referee;
import org.example.model.Team;

import java.util.ArrayList;
import java.util.List;

// Builds players, teams and referees for the tests, so no test needs the database.
public final class TestData {

    private static int nextId = 1;

    // The squad of every test team: 2 goalkeepers, 7 defenders, 7 midfielders, 4 forwards
    private static final DetailedPosition[] SQUAD = {
            DetailedPosition.GK, DetailedPosition.GK,
            DetailedPosition.LB, DetailedPosition.CB, DetailedPosition.CB, DetailedPosition.CB, DetailedPosition.RB,
            DetailedPosition.LB, DetailedPosition.RB,
            DetailedPosition.CDM, DetailedPosition.CM, DetailedPosition.CM, DetailedPosition.CM, DetailedPosition.CAM,
            DetailedPosition.CDM, DetailedPosition.CM,
            DetailedPosition.LW, DetailedPosition.ST, DetailedPosition.RW, DetailedPosition.ST
    };

    private TestData() {
    }

    // Every attribute has the same value: easy to reason about in the tests
    public static PlayerAttributes attributes(int value) {
        return new PlayerAttributes(value, value, value, value, value, value, value, value, value, value, value);
    }

    public static Player player(DetailedPosition position, int rating) {
        int id = nextId++;
        return new Player(id, "Test", "Player " + id, "Nowhere", 25, 1 + id % 99, position,
                PreferredFoot.RIGHT, attributes(rating));
    }

    public static Player player() {
        return player(DetailedPosition.CM, 75);
    }

    public static Team team(String name, int rating) {
        int id = nextId++;
        Coach coach = new Coach(nextId++, "Test", "Coach " + id, "Nowhere", 50, Formation.FOUR_THREE_THREE);
        Team team = new Team(id, name, name.substring(0, 3), "City", "Stadium", coach);
        for (DetailedPosition position : SQUAD) {
            team.addPlayer(player(position, rating));
        }
        return team;
    }

    // Teams called "Team 01", "Team 02"... all with the same rating
    public static List<Team> league(int size, int rating) {
        List<Team> teams = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            teams.add(team(String.format("Team %02d", i), rating));
        }
        return teams;
    }

    public static List<Referee> referees(int count) {
        List<Referee> referees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int id = nextId++;
            referees.add(new Referee(id, "Test", "Referee " + id, "Nowhere", 40, 68));
        }
        return referees;
    }
}
