package com.vitorbetmann.schoolapi.infrastructure.dtos;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;

import java.time.Instant;

public record SubmitFeedbackResponse(String id, Urgency urgency, Instant submittedAt) {

    public static SubmitFeedbackResponse fromDomain(Feedback feedback) {
        return new SubmitFeedbackResponse(feedback.id(), feedback.urgency(), feedback.submittedAt());
    }
}
