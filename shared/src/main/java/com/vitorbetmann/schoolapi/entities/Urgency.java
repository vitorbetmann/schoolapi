package com.vitorbetmann.schoolapi.entities;

public enum Urgency {
    CRITICAL,
    MEDIUM,
    LOW;

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 10;
    private static final int CRITICAL_MAX_SCORE = 3;
    private static final int MEDIUM_MAX_SCORE = 6;

    public static Urgency fromScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException("Feedback score out of range: " + score);
        }
        if (score <= CRITICAL_MAX_SCORE) {
            return CRITICAL;
        }
        if (score <= MEDIUM_MAX_SCORE) {
            return MEDIUM;
        }
        return LOW;
    }
}
