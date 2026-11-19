package com.vitorbetmann.schoolapi.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record WeeklyReport(
        ReportPeriod period,
        List<Feedback> items,
        Optional<BigDecimal> averageScore,
        Map<LocalDate, Long> countPerDay,
        Map<Urgency, Long> countPerUrgency) {

    private static final int AVERAGE_SCALE = 2;

    public static WeeklyReport from(List<Feedback> feedbacks, ReportPeriod period) {
        for (var feedback : feedbacks) {
            if (!period.contains(feedback.day())) {
                throw new IllegalArgumentException(
                        "Feedback " + feedback.id() + " is outside the report period: " + feedback.day());
            }
        }

        var items = feedbacks.stream().sorted(Comparator.comparing(Feedback::submittedAt)).toList();

        var countPerDay = new LinkedHashMap<LocalDate, Long>();
        period.days().forEach(day -> countPerDay.put(day, 0L));
        var countPerUrgency = new EnumMap<Urgency, Long>(Urgency.class);
        for (var urgency : Urgency.values()) {
            countPerUrgency.put(urgency, 0L);
        }
        for (var feedback : items) {
            countPerDay.merge(feedback.day(), 1L, Long::sum);
            countPerUrgency.merge(feedback.urgency(), 1L, Long::sum);
        }

        return new WeeklyReport(period, items, averageOf(items),
                Collections.unmodifiableMap(countPerDay), Collections.unmodifiableMap(countPerUrgency));
    }

    private static Optional<BigDecimal> averageOf(List<Feedback> items) {
        if (items.isEmpty()) {
            return Optional.empty();
        }
        var total = items.stream().mapToLong(Feedback::score).sum();
        return Optional.of(BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(items.size()), AVERAGE_SCALE, RoundingMode.HALF_UP));
    }
}
