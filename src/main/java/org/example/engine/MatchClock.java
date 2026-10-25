package org.example.engine;

import org.example.config.EngineConfig;
import org.example.engine.random.RandomProvider;

public class MatchClock {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int NOT_ANNOUNCED = -1;

    private MatchPeriod period = MatchPeriod.FIRST_HALF;
    private int secondsInPeriod;
    private int stoppageMinutes = NOT_ANNOUNCED;

    private int goals;
    private int substitutions;
    private int cards;
    private int injuries;

    public void advance(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Time cannot go backwards");
        }
        secondsInPeriod += seconds;
    }

    public MatchMinute getMinute() {
        int regulationSeconds = period.getLengthInSeconds();
        if (secondsInPeriod < regulationSeconds) {
            return MatchMinute.of(period.getStartMinute() + secondsInPeriod / SECONDS_PER_MINUTE + 1);
        }
        int addedTime = (secondsInPeriod - regulationSeconds) / SECONDS_PER_MINUTE + 1;
        return new MatchMinute(period.getEndMinute(), addedTime);
    }

    public boolean isRegulationTimeOver() {
        return secondsInPeriod >= period.getLengthInSeconds();
    }

    public boolean isStoppageTimeAnnounced() {
        return stoppageMinutes != NOT_ANNOUNCED;
    }

    public int announceStoppageTime(RandomProvider random) {
        if (!isRegulationTimeOver()) {
            throw new IllegalStateException("Stoppage time is announced only after " + period.getEndMinute() + "'");
        }
        if (isStoppageTimeAnnounced()) {
            return stoppageMinutes;
        }
        double minutes = EngineConfig.STOPPAGE_BASE_MINUTES
                + EngineConfig.STOPPAGE_PER_EVENT * (goals + substitutions + cards)
                + EngineConfig.STOPPAGE_PER_INJURY * injuries
                + random.nextDouble();
        int min = period == MatchPeriod.FIRST_HALF
                ? EngineConfig.MIN_STOPPAGE_FIRST_HALF : EngineConfig.MIN_STOPPAGE_SECOND_HALF;
        int max = period == MatchPeriod.FIRST_HALF
                ? EngineConfig.MAX_STOPPAGE_FIRST_HALF : EngineConfig.MAX_STOPPAGE_SECOND_HALF;
        stoppageMinutes = Math.max(min, Math.min(max, (int) Math.round(minutes)));
        return stoppageMinutes;
    }

    public boolean isPeriodOver() {
        if (!isStoppageTimeAnnounced()) {
            return false;
        }
        int totalSeconds = period.getLengthInSeconds() + stoppageMinutes * SECONDS_PER_MINUTE;
        return secondsInPeriod >= totalSeconds;
    }

    public void startSecondHalf() {
        if (period != MatchPeriod.FIRST_HALF) {
            throw new IllegalStateException("The second half has already started");
        }
        period = MatchPeriod.SECOND_HALF;
        secondsInPeriod = 0;
        stoppageMinutes = NOT_ANNOUNCED;
        goals = 0;
        substitutions = 0;
        cards = 0;
        injuries = 0;
    }

    public void recordGoal() { goals++; }
    public void recordSubstitution() { substitutions++; }
    public void recordCard() { cards++; }
    public void recordInjury() { injuries++; }

    public MatchPeriod getPeriod() { return period; }
    public int getSecondsInPeriod() { return secondsInPeriod; }
    public int getStoppageMinutes() { return isStoppageTimeAnnounced() ? stoppageMinutes : 0; }
}