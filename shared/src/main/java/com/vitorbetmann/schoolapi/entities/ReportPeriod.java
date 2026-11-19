package com.vitorbetmann.schoolapi.entities;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Objects;

public record ReportPeriod(LocalDate start, LocalDate end) {

    private static final int DAYS_IN_WEEK = 7;

    public ReportPeriod {
        Objects.requireNonNull(start, "Report period start is required");
        Objects.requireNonNull(end, "Report period end is required");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Report period end is before start: " + start + " > " + end);
        }
    }

    public static ReportPeriod weekBefore(LocalDate referenceDate) {
        return new ReportPeriod(referenceDate.minusDays(DAYS_IN_WEEK), referenceDate.minusDays(1));
    }

    public List<LocalDate> days() {
        return this.start.datesUntil(this.end.plusDays(1)).toList();
    }

    public boolean contains(LocalDate day) {
        return !day.isBefore(this.start) && !day.isAfter(this.end);
    }

    public String isoWeek() {
        return String.format("%d-W%02d",
                this.start.get(IsoFields.WEEK_BASED_YEAR),
                this.start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}
