package org.example;

import org.example.model.Coach;
import org.example.model.DetailedPosition;
import org.example.model.FieldRole;
import org.example.model.Formation;
import org.example.model.Person;
import org.example.model.Player;
import org.example.model.PlayerAttributes;
import org.example.model.PlayerStatus;
import org.example.model.PreferredFoot;
import org.example.model.Referee;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        testDatabaseConnection();
        testPlayer();
        testFieldRoles();
        testCondition();
        testStatus();
        testValidation();
        testFormations();
        testCoachAndReferee();
        testPolymorphism();
        testOverall();
    }

    private static void testOverall() {
        printTitle("Overall (OVR)");
        Player messi = createMessi();
        Player buffon = createBuffon();

        System.out.println(messi.getFullName() + ": " + messi.getOverall() + " OVR (expected 88)");
        System.out.println(buffon.getFullName() + ": " + buffon.getOverall() + " OVR (expected 89)");

        System.out.println();
        System.out.println("Messi on every field role:");
        for (FieldRole fieldRole : FieldRole.values()) {
            System.out.println("  " + fieldRole + ": " + buffon.getOverallAs(fieldRole));
        }
    }

    private static void testDatabaseConnection() {
        printTitle("Database connection");
        Properties dbProperties = loadDatabaseProperties();

        String url = dbProperties.getProperty("db.url");
        String user = dbProperties.getProperty("db.user");
        String password = dbProperties.getProperty("db.password");

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to database: " + connection.getCatalog());
        } catch (SQLException e) {
            System.out.println("Could not connect to database: " + e.getMessage());
        }
    }

    private static void testPlayer() {
        printTitle("Player");
        Player messi = createMessi();

        System.out.println(messi);
        System.out.println("Full name: " + messi.getFullName());
        System.out.println("Age: " + messi.getAge());
        System.out.println("Position: " + messi.getPosition());
        System.out.println("Field role: " + messi.getFieldRole());
        System.out.println("Preferred foot: " + messi.getPreferredFoot());
        System.out.println("Shooting: " + messi.getAttributes().shooting());
        System.out.println("Dribbling: " + messi.getAttributes().dribbling());
    }

    private static void testFieldRoles() {
        printTitle("Field roles");
        for (DetailedPosition position : DetailedPosition.values()) {
            System.out.println(position + " -> " + position.getFieldRole());
        }

        boolean correct = DetailedPosition.ST.getFieldRole() == FieldRole.FORWARD
                && DetailedPosition.CB.getFieldRole() == FieldRole.DEFENDER
                && DetailedPosition.CM.getFieldRole() == FieldRole.MIDFIELDER
                && DetailedPosition.GK.getFieldRole() == FieldRole.GOALKEEPER;
        System.out.println("Check: " + (correct ? "OK" : "FAILED"));
    }

    private static void testCondition() {
        printTitle("Condition");
        Player messi = createMessi();

        System.out.println("Start: " + messi.getCondition());
        messi.reduceCondition(30);
        System.out.println("After match (-30): " + messi.getCondition());
        messi.recoverCondition(50);
        System.out.println("After recovery (+50): " + messi.getCondition() + " (max is 100)");
        messi.reduceCondition(150);
        System.out.println("After -150: " + messi.getCondition() + " (min is 0)");
    }

    private static void testStatus() {
        printTitle("Status");
        Player messi = createMessi();

        System.out.println("Status: " + messi.getStatus() + ", available: " + messi.isAvailable());
        messi.setStatus(PlayerStatus.INJURED);
        System.out.println("Status: " + messi.getStatus() + ", available: " + messi.isAvailable());
    }

    private static void testValidation() {
        printTitle("Validation (every line should be an expected error)");

        try {
            new PlayerAttributes(87, 120, 91, 96, 35, 68, 70, 10, 10, 85, 30);
            System.out.println("FAILED: attribute 120 was accepted");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        try {
            new Player(2, "Test", "Player", "Romania", 25,
                    150, DetailedPosition.ST, PreferredFoot.RIGHT, createMessi().getAttributes());
            System.out.println("FAILED: shirt number 150 was accepted");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        try {
            new Player(3, "Test", "", "Romania", 25,
                    9, DetailedPosition.ST, PreferredFoot.RIGHT, createMessi().getAttributes());
            System.out.println("FAILED: empty last name was accepted");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        try {
            new Player(4, "Test", "Player", "Romania", 25,
                    9, null, PreferredFoot.RIGHT, createMessi().getAttributes());
            System.out.println("FAILED: null position was accepted");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        try {
            new Referee(5, "Test", "Referee", "Romania", 40, 150);
            System.out.println("FAILED: strictness 150 was accepted");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        try {
            new Coach(6, "Test", "Coach", "Romania", 50, null);
            System.out.println("FAILED: null formation was accepted");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error: " + e.getMessage());
        }
    }

    private static void testFormations() {
        printTitle("Formations");
        for (Formation formation : Formation.values()) {
            System.out.println(formation
                    + " | GK: " + formation.countPlayers(FieldRole.GOALKEEPER)
                    + ", DEF: " + formation.countPlayers(FieldRole.DEFENDER)
                    + ", MID: " + formation.countPlayers(FieldRole.MIDFIELDER)
                    + ", FWD: " + formation.countPlayers(FieldRole.FORWARD)
                    + " | " + formation.getSlots());
        }
    }

    private static void testCoachAndReferee() {
        printTitle("Coach and referee");
        Coach guardiola = createGuardiola();
        Referee collina = createCollina();

        System.out.println(guardiola);
        System.out.println("Preferred formation: " + guardiola.getPreferredFormation());
        System.out.println(collina);
        System.out.println("Strictness: " + collina.getStrictness());
    }

    private static void testPolymorphism() {
        printTitle("Polymorphism");
        List<Person> people = List.of(createMessi(), createBuffon(), createGuardiola(), createCollina());

        for (Person person : people) {
            System.out.println(person + " -> role: " + person.getRole());
        }
    }

    private static Player createMessi() {
        PlayerAttributes attributes = new PlayerAttributes(
                87, // pace
                92, // shooting
                91, // passing
                96, // dribbling
                35, // defending
                68, // physical
                70, // heading
                10, // reflexes
                10, // handling
                85, // stamina
                30  // aggression
        );
        return new Player(1, "Lionel", "Messi", "Argentina", 25,
                10, DetailedPosition.RW, PreferredFoot.LEFT, attributes);
    }

    private static Player createBuffon() {
        PlayerAttributes attributes = new PlayerAttributes(
                50, // pace
                20, // shooting
                60, // passing
                30, // dribbling
                25, // defending
                80, // physical
                40, // heading
                92, // reflexes
                90, // handling
                75, // stamina
                40  // aggression
        );
        return new Player(5, "Gianluigi", "Buffon", "Italy", 28,
                1, DetailedPosition.GK, PreferredFoot.RIGHT, attributes);
    }

    private static Coach createGuardiola() {
        return new Coach(1, "Pep", "Guardiola", "Spain", 41, Formation.FOUR_THREE_THREE);
    }

    private static Referee createCollina() {
        return new Referee(1, "Pierluigi", "Collina", "Italy", 42, 85);
    }

    private static void printTitle(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static Properties loadDatabaseProperties() {
        Properties properties = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IllegalStateException("db.properties not found in src/main/resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read db.properties", e);
        }
        return properties;
    }
}