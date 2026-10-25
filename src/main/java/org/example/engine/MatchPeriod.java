package org.example.engine;

public enum MatchPeriod {
    FIRST_HALF(0, 45, "First half"),
    SECOND_HALF(45, 90, "Second half");

    private final int startMinute;
    private final int endMinute;
    private final String displayName;

    MatchPeriod(int startMinute, int endMinute, String displayName) {
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.displayName = displayName;
    }

    public int getStartMinute() { return startMinute; }
    public int getEndMinute() { return endMinute; }
    public int getLengthInSeconds() { return (endMinute - startMinute) * 60; }

    @Override
    public String toString() {
        return displayName;
    }
}