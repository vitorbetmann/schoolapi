package com.vitorbetmann.schoolapi.entities;

import com.vitorbetmann.schoolapi.usecases.exceptions.InvalidFeedbackException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public record Feedback(String id, String description, int score, Urgency urgency, Instant submittedAt, LocalDate day) {

    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final ZoneId REPORTING_ZONE = ZoneId.of("America/Sao_Paulo");

    public Feedback {
        Objects.requireNonNull(id, "Feedback id is required");
        Objects.requireNonNull(description, "Feedback description is required");
        Objects.requireNonNull(urgency, "Feedback urgency is required");
        Objects.requireNonNull(submittedAt, "Feedback submittedAt is required");
        Objects.requireNonNull(day, "Feedback day is required");
    }

    public static Feedback create(String description, Integer score, Clock clock) {
        var violations = new ArrayList<String>();
        if (description == null || description.isBlank()) {
            violations.add("Feedback description is required");
        } else if (description.length() > MAX_DESCRIPTION_LENGTH) {
            violations.add("Feedback description exceeds " + MAX_DESCRIPTION_LENGTH + " characters: "
                    + description.length());
        }
        if (score == null) {
            violations.add("Feedback score is required");
        } else if (score < Urgency.MIN_SCORE || score > Urgency.MAX_SCORE) {
            violations.add("Feedback score out of range: " + score);
        }
        if (!violations.isEmpty()) {
            throw new InvalidFeedbackException(violations);
        }

        var submittedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        return new Feedback(UUID.randomUUID().toString(), description, score, Urgency.fromScore(score),
                submittedAt, LocalDate.ofInstant(submittedAt, REPORTING_ZONE));
    }
}
