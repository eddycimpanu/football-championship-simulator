package org.example.model;

public class Referee extends Person {

    public static final int MIN_STRICTNESS = 1;
    public static final int MAX_STRICTNESS = 99;

    private final int strictness;

    public Referee(int id, String firstName, String lastName, String nationality, int age,
                   int strictness) {
        super(id, firstName, lastName, nationality, age);
        if (strictness < MIN_STRICTNESS || strictness > MAX_STRICTNESS) {
            throw new IllegalArgumentException(
                    "Strictness must be between " + MIN_STRICTNESS + " and " + MAX_STRICTNESS + ", got " + strictness);
        }
        this.strictness = strictness;
    }

    @Override
    public String getRole() {
        return "Referee";
    }

    public int getStrictness() {
        return strictness;
    }

    @Override
    public String toString() {
        return getFullName() + " (Referee, strictness " + strictness + ")";
    }
}