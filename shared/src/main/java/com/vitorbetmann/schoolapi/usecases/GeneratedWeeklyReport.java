package com.vitorbetmann.schoolapi.usecases;

import com.vitorbetmann.schoolapi.entities.WeeklyReport;

/**
 * Result of {@link GenerateWeeklyReport}: the aggregated report and where it was stored.
 *
 * @param report   the aggregated weekly report
 * @param location the storage location returned by the report storage gateway
 */
public record GeneratedWeeklyReport(WeeklyReport report, String location) {
}
