package com.vitorbetmann.schoolapi.entities;

import com.vitorbetmann.schoolapi.usecases.exceptions.InvalidFeedbackException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class FeedbackTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T13:45:00.123456Z"), ZoneOffset.UTC);

    @Test
    void createsFeedbackWithUrgencyAndTimestamps() {
        var feedback = Feedback.create("Ótima aula", 9, this.clock);

        assertThat(feedback.id()).isNotBlank();
        assertThat(feedback.description()).isEqualTo("Ótima aula");
        assertThat(feedback.score()).isEqualTo(9);
        assertThat(feedback.urgency()).isEqualTo(Urgency.LOW);
        assertThat(feedback.submittedAt()).isEqualTo(Instant.parse("2026-11-18T13:45:00.123Z"));
        assertThat(feedback.day()).isEqualTo(LocalDate.parse("2026-11-18"));
    }

    @Test
    void generatesUniqueIds() {
        var first = Feedback.create("Aula boa", 8, this.clock);
        var second = Feedback.create("Aula boa", 8, this.clock);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void usesSaoPauloDateForDay() {
        var lateEveningInSaoPaulo = Clock.fixed(Instant.parse("2026-11-19T01:30:00Z"), ZoneOffset.UTC);

        var feedback = Feedback.create("Aula boa", 8, lateEveningInSaoPaulo);

        assertThat(feedback.day()).isEqualTo(LocalDate.parse("2026-11-18"));
    }

    @ParameterizedTest
    @CsvSource({"0,CRITICAL", "3,CRITICAL", "4,MEDIUM", "6,MEDIUM", "7,LOW", "10,LOW"})
    void acceptsBoundaryScores(int score, Urgency expected) {
        var feedback = Feedback.create("Aula", score, this.clock);

        assertThat(feedback.urgency()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void rejectsScoreOutOfRange(int score) {
        var exception = catchThrowableOfType(InvalidFeedbackException.class,
                () -> Feedback.create("Aula", score, this.clock));

        assertThat(exception.violations()).containsExactly("Feedback score out of range: " + score);
    }

    @Test
    void rejectsMissingScore() {
        var exception = catchThrowableOfType(InvalidFeedbackException.class,
                () -> Feedback.create("Aula", null, this.clock));

        assertThat(exception.violations()).containsExactly("Feedback score is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankDescription(String description) {
        var exception = catchThrowableOfType(InvalidFeedbackException.class,
                () -> Feedback.create(description, 5, this.clock));

        assertThat(exception.violations()).containsExactly("Feedback description is required");
    }

    @Test
    void acceptsDescriptionAtMaxLength() {
        var description = "a".repeat(Feedback.MAX_DESCRIPTION_LENGTH);

        assertThat(Feedback.create(description, 5, this.clock).description()).hasSize(1000);
    }

    @Test
    void rejectsDescriptionOverMaxLength() {
        var exception = catchThrowableOfType(InvalidFeedbackException.class,
                () -> Feedback.create("a".repeat(1001), 5, this.clock));

        assertThat(exception.violations()).containsExactly("Feedback description exceeds 1000 characters: 1001");
    }

    @Test
    void collectsEveryViolation() {
        var exception = catchThrowableOfType(InvalidFeedbackException.class,
                () -> Feedback.create(null, 11, this.clock));

        assertThat(exception.violations()).containsExactly(
                "Feedback description is required",
                "Feedback score out of range: 11");
    }
}
