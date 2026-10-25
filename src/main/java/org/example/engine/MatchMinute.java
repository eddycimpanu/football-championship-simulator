package org.example.engine;

public record MatchMinute(int minute, int addedTime) implements Comparable<MatchMinute> {

    public MatchMinute {
        if (minute < 1 || minute > 90) {
            throw new IllegalArgumentException("Minute must be between 1 and 90, got " + minute);
        }
        if (addedTime < 0) {
            throw new IllegalArgumentException("Added time cannot be negative");
        }
        if (addedTime > 0 && minute != 45 && minute != 90) {
            throw new IllegalArgumentException("Added time only exists at 45' and 90', got " + minute + "+" + addedTime);
        }
    }

    public static MatchMinute of(int minute) {
        return new MatchMinute(minute, 0);
    }

    @Override
    public int compareTo(MatchMinute other) {
        int byMinute = Integer.compare(minute, other.minute);
        return byMinute != 0 ? byMinute : Integer.compare(addedTime, other.addedTime);
    }

    @Override
    public String toString() {
        return addedTime > 0 ? minute + "+" + addedTime + "'" : minute + "'";
    }
}