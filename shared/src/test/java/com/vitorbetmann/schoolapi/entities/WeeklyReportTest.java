package com.vitorbetmann.schoolapi.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class WeeklyReportTest {

    private static final ReportPeriod PERIOD = ReportPeriod.weekBefore(LocalDate.parse("2026-11-23"));

    @Test
    void averagesScoresRoundedToTwoDecimals() {
        var feedbacks = List.of(
                feedback("2026-11-16", "10:00:00", 1),
                feedback("2026-11-17", "10:00:00", 2),
                feedback("2026-11-18", "10:00:00", 2));

        var report = WeeklyReport.from(feedbacks, PERIOD);

        assertThat(report.averageScore()).contains(new BigDecimal("1.67"));
    }

    @Test
    void countsPerDayIncludingDaysWithoutFeedback() {
        var feedbacks = List.of(
                feedback("2026-11-16", "10:00:00", 5),
                feedback("2026-11-16", "11:00:00", 8),
                feedback("2026-11-20", "10:00:00", 2));

        var report = WeeklyReport.from(feedbacks, PERIOD);

        assertThat(report.countPerDay()).containsExactly(
                entry(LocalDate.parse("2026-11-16"), 2L),
                entry(LocalDate.parse("2026-11-17"), 0L),
                entry(LocalDate.parse("2026-11-18"), 0L),
                entry(LocalDate.parse("2026-11-19"), 0L),
                entry(LocalDate.parse("2026-11-20"), 1L),
                entry(LocalDate.parse("2026-11-21"), 0L),
                entry(LocalDate.parse("2026-11-22"), 0L));
    }

    @Test
    void countsPerUrgencyIncludingUrgenciesWithoutFeedback() {
        var feedbacks = List.of(
                feedback("2026-11-16", "10:00:00", 2),
                feedback("2026-11-17", "10:00:00", 5));

        var report = WeeklyReport.from(feedbacks, PERIOD);

        assertThat(report.countPerUrgency()).containsExactly(
                entry(Urgency.CRITICAL, 1L),
                entry(Urgency.MEDIUM, 1L),
                entry(Urgency.LOW, 0L));
    }

    @Test
    void listsItemsInSubmissionOrder() {
        var later = feedback("2026-11-18", "09:00:00", 7);
        var earlier = feedback("2026-11-16", "09:00:00", 3);

        var report = WeeklyReport.from(List.of(later, earlier), PERIOD);

        assertThat(report.items()).containsExactly(earlier, later);
    }

    @Test
    void reportsAnEmptyWeek() {
        var report = WeeklyReport.from(List.of(), PERIOD);

        assertThat(report.items()).isEmpty();
        assertThat(report.averageScore()).isEmpty();
        assertThat(report.countPerDay()).hasSize(7).allSatisfy((day, count) -> assertThat(count).isZero());
        assertThat(report.countPerUrgency()).hasSize(3).allSatisfy((urgency, count) -> assertThat(count).isZero());
    }

    @Test
    void rejectsFeedbackOutsideThePeriod() {
        var outside = feedback("2026-11-23", "10:00:00", 5);

        assertThatThrownBy(() -> WeeklyReport.from(List.of(outside), PERIOD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feedback " + outside.id() + " is outside the report period: 2026-11-23");
    }

    private static Feedback feedback(String day, String time, int score) {
        return new Feedback(UUID.randomUUID().toString(), "Aula", score, Urgency.fromScore(score),
                Instant.parse(day + "T" + time + "Z"), LocalDate.parse(day));
    }
}
