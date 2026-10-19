package org.example.model;

public class Player extends Person {

    public static final int MAX_CONDITION = 100;

    private final int shirtNumber;
    private final DetailedPosition position;
    private final PreferredFoot preferredFoot;
    private final PlayerAttributes attributes;

    private int condition;
    private PlayerStatus status;

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
        this.status = PlayerStatus.AVAILABLE;
    }

    @Override
    public String getRole() {
        return "Player";
    }

    public FieldRole getFieldRole() {
        return position.getFieldRole();
    }

    public boolean isAvailable() {
        return status == PlayerStatus.AVAILABLE;
    }

    public void reduceCondition(int amount) {
        condition = Math.max(0, condition - Math.abs(amount));
    }

    public void recoverCondition(int amount) {
        condition = Math.min(MAX_CONDITION, condition + Math.abs(amount));
    }

    public void setStatus(PlayerStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
    }

    public int getShirtNumber() { return shirtNumber; }
    public DetailedPosition getPosition() { return position; }
    public PreferredFoot getPreferredFoot() { return preferredFoot; }
    public PlayerAttributes getAttributes() { return attributes; }
    public int getCondition() { return condition; }
    public PlayerStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "#" + shirtNumber + " " + getFullName() + " (" + position + ", " + getNationality() + ")";
    }
}