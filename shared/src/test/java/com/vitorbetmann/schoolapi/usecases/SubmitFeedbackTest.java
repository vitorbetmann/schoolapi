package com.vitorbetmann.schoolapi.usecases;

import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.usecases.exceptions.InvalidFeedbackException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmitFeedbackTest {

    private final InMemoryFeedbackGateway feedbackGateway = new InMemoryFeedbackGateway();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T13:45:00Z"), ZoneOffset.UTC);
    private final SubmitFeedback submitFeedback = new SubmitFeedback(this.feedbackGateway, this.clock);

    @Test
    void storesAndReturnsTheClassifiedFeedback() {
        var feedback = this.submitFeedback.execute("Aula confusa", 2);

        assertThat(feedback.urgency()).isEqualTo(Urgency.CRITICAL);
        assertThat(feedback.submittedAt()).isEqualTo(Instant.parse("2026-11-18T13:45:00Z"));
        assertThat(this.feedbackGateway.saved).containsExactly(feedback);
    }

    @Test
    void storesNothingWhenFeedbackIsInvalid() {
        assertThatThrownBy(() -> this.submitFeedback.execute("", 11))
                .isInstanceOf(InvalidFeedbackException.class);

        assertThat(this.feedbackGateway.saved).isEmpty();
    }
}
