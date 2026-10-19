package org.example.model;

public abstract class Person {

    private final int id;
    private final String firstName;
    private final String lastName;
    private final String nationality;
    private final int age;

    protected Person(int id, String firstName, String lastName, String nationality, int age) {
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (age < 15 || age > 80) {
            throw new IllegalArgumentException("Invalid age: " + age);
        }
        this.id = id;
        this.firstName = firstName == null ? "" : firstName;
        this.lastName = lastName;
        this.nationality = nationality;
        this.age = age;
    }

    public abstract String getRole();

    public String getFullName() {
        return firstName.isBlank() ? lastName : firstName + " " + lastName;
    }

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getNationality() { return nationality; }
    public int getAge() { return age; }

    @Override
    public String toString() {
        return getFullName() + " (" + getRole() + ")";
    }
}