package com.vitorbetmann.schoolapi.infrastructure.dtos;

import com.vitorbetmann.schoolapi.entities.Feedback;

import java.time.Clock;
import java.time.LocalDate;

public record GenerateWeeklyReportRequest(LocalDate referenceDate) {

    public LocalDate referenceDateOr(Clock clock) {
        return this.referenceDate != null ? this.referenceDate : LocalDate.now(clock.withZone(Feedback.REPORTING_ZONE));
    }
}
