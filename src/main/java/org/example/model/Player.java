package org.example.model;

public class Player extends Person {

    public static final int MAX_CONDITION = 100;
    public static final int YELLOW_CARDS_FOR_BAN = 5;

    private final int shirtNumber;
    private final DetailedPosition position;
    private final PreferredFoot preferredFoot;
    private final PlayerAttributes attributes;

    private int condition;
    private int yellowCardsAccumulated;
    private int matchesSuspended;
    private int matchesInjured;

    public Player(int id, String firstName, String lastName, String nationality, int age,
                  int shirtNumber, DetailedPosition position, PreferredFoot preferredFoot,
                  PlayerAttributes attributes) {
        super(id, firstName, lastName, nationality, age);
        if (shirtNumber < 1 || shirtNumber > 99) {
            throw new IllegalArgumentException("Shirt number must be between 1 and 99, got " + shirtNumber);
        }
        if (position == null || attributes == null) {
            throw new IllegalArgumentException("Position and attributes are required");
        }
        this.shirtNumber = shirtNumber;
        this.position = position;
        this.preferredFoot = preferredFoot == null ? PreferredFoot.RIGHT : preferredFoot;
        this.attributes = attributes;
        this.condition = MAX_CONDITION;
    }

    @Override
    public String getRole() {
        return "Player";
    }

    public FieldRole getFieldRole() {
        return position.getFieldRole();
    }

    public int getOverall() {
        return getOverallAs(position);
    }

    public int getOverallAs(DetailedPosition otherPosition) {
        return OverallCalculators.forPosition(otherPosition).calculate(attributes);
    }

    // ----- Condition -----

    public void reduceCondition(int amount) {
        condition = Math.max(0, condition - Math.abs(amount));
    }

    public void recoverCondition(int amount) {
        condition = Math.min(MAX_CONDITION, condition + Math.abs(amount));
    }

    // ----- Discipline -----

    // Returns true when this yellow card completes a ban (5 yellows); the counter then starts again.
    public boolean addYellowCard() {
        yellowCardsAccumulated++;
        if (yellowCardsAccumulated >= YELLOW_CARDS_FOR_BAN) {
            yellowCardsAccumulated = 0;
            suspend(1);
            return true;
        }
        return false;
    }

    public void suspend(int matches) {
        if (matches <= 0) {
            throw new IllegalArgumentException("A ban must be at least one match");
        }
        matchesSuspended += matches;
    }

    public void injure(int matches) {
        if (matches <= 0) {
            throw new IllegalArgumentException("An injury must last at least one match");
        }
        matchesInjured = Math.max(matchesInjured, matches);
    }

    // Called once for every match of his team that he could not play.
    public void missMatch() {
        if (matchesSuspended > 0) {
            matchesSuspended--;
        }
        if (matchesInjured > 0) {
            matchesInjured--;
        }
    }

    public PlayerStatus getStatus() {
        if (matchesInjured > 0) {
            return PlayerStatus.INJURED;
        }
        if (matchesSuspended > 0) {
            return PlayerStatus.SUSPENDED;
        }
        return PlayerStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return getStatus() == PlayerStatus.AVAILABLE;
    }

    // ----- Getters -----

    public int getShirtNumber() { return shirtNumber; }
    public DetailedPosition getPosition() { return position; }
    public PreferredFoot getPreferredFoot() { return preferredFoot; }
    public PlayerAttributes getAttributes() { return attributes; }
    public int getCondition() { return condition; }
    public int getYellowCardsAccumulated() { return yellowCardsAccumulated; }
    public int getMatchesSuspended() { return matchesSuspended; }
    public int getMatchesInjured() { return matchesInjured; }

    @Override
    public String toString() {
        return "#" + shirtNumber + " " + getFullName() + " (" + position + ", " + getOverall() + " OVR)";
    }
}
