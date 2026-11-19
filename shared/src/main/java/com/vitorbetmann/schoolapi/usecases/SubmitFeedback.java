package com.vitorbetmann.schoolapi.usecases;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.usecases.exceptions.InvalidFeedbackException;
import com.vitorbetmann.schoolapi.usecases.interfaces.FeedbackGateway;

import java.time.Clock;

/**
 * Validates a student's feedback, classifies its urgency and stores it.
 */
public class SubmitFeedback {

    private final FeedbackGateway feedbackGateway;
    private final Clock clock;

    public SubmitFeedback(FeedbackGateway feedbackGateway, Clock clock) {
        this.feedbackGateway = feedbackGateway;
        this.clock = clock;
    }

    /**
     * Creates and stores a feedback.
     *
     * @param description the student's comment
     * @param score       the score from 0 to 10, or {@code null} if it was not sent
     * @return the stored feedback, with its id, urgency and submission time
     * @throws InvalidFeedbackException if the description or score is invalid; nothing is stored
     */
    public Feedback execute(String description, Integer score) {
        var feedback = Feedback.create(description, score, this.clock);
        this.feedbackGateway.save(feedback);
        return feedback;
    }
}
