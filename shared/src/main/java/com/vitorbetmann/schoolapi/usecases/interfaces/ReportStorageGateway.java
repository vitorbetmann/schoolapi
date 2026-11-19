package com.vitorbetmann.schoolapi.usecases.interfaces;

import com.vitorbetmann.schoolapi.entities.WeeklyReport;

/**
 * Storage port for generated weekly reports.
 */
public interface ReportStorageGateway {

    /**
     * Stores a weekly report, replacing any report already stored for the same period.
     *
     * @param report the report to store
     * @return the location of the stored report (for example, its object key)
     */
    String save(WeeklyReport report);
}
