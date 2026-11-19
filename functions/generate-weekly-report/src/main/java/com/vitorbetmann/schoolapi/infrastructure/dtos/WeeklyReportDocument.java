package com.vitorbetmann.schoolapi.infrastructure.dtos;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.entities.WeeklyReport;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyReportDocument(
        String isoWeek,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal averageScore,
        int feedbackCount,
        Map<LocalDate, Long> countPerDay,
        Map<Urgency, Long> countPerUrgency,
        List<Item> items) {

    public static WeeklyReportDocument fromDomain(WeeklyReport report) {
        var period = report.period();
        return new WeeklyReportDocument(period.isoWeek(), period.start(), period.end(),
                report.averageScore().orElse(null), report.items().size(),
                report.countPerDay(), report.countPerUrgency(),
                report.items().stream().map(Item::fromDomain).toList());
    }

    public record Item(String id, String description, int score, Urgency urgency, Instant submittedAt, LocalDate day) {

        static Item fromDomain(Feedback feedback) {
            return new Item(feedback.id(), feedback.description(), feedback.score(), feedback.urgency(),
                    feedback.submittedAt(), feedback.day());
        }
    }
}
