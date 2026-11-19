package com.vitorbetmann.schoolapi.infrastructure.dtos;

import com.vitorbetmann.schoolapi.usecases.GeneratedWeeklyReport;

public record GenerateWeeklyReportResponse(String isoWeek, String location, int feedbackCount) {

    public static GenerateWeeklyReportResponse fromDomain(GeneratedWeeklyReport generated) {
        var report = generated.report();
        return new GenerateWeeklyReportResponse(report.period().isoWeek(), generated.location(), report.items().size());
    }
}
