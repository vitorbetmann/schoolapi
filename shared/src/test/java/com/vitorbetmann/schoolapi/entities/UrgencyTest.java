package com.vitorbetmann.schoolapi.entities;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrgencyTest {

    @ParameterizedTest
    @CsvSource({"0,CRITICAL", "3,CRITICAL", "4,MEDIUM", "6,MEDIUM", "7,LOW", "10,LOW"})
    void classifiesScoreIntoUrgency(int score, Urgency expected) {
        assertThat(Urgency.fromScore(score)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void rejectsScoreOutOfRange(int score) {
        assertThatThrownBy(() -> Urgency.fromScore(score))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feedback score out of range: " + score);
    }
}
