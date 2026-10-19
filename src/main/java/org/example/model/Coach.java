package org.example.model;

public class Coach extends Person {

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

    public Formation getPreferredFormation() {
        return preferredFormation;
    }

    @Override
    public String toString() {
        return getFullName() + " (Coach, " + preferredFormation + ")";
    }
}