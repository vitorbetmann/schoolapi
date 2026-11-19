package com.vitorbetmann.schoolapi.usecases;

import com.vitorbetmann.schoolapi.entities.ReportPeriod;
import com.vitorbetmann.schoolapi.entities.WeeklyReport;
import com.vitorbetmann.schoolapi.usecases.interfaces.FeedbackGateway;
import com.vitorbetmann.schoolapi.usecases.interfaces.NotificationGateway;
import com.vitorbetmann.schoolapi.usecases.interfaces.ReportStorageGateway;
import com.vitorbetmann.schoolapi.usecases.messages.PtBrMessages;

import java.time.LocalDate;

/**
 * Builds the weekly feedback report, stores it and sends the administrators a summary.
 */
public class GenerateWeeklyReport {

    private final FeedbackGateway feedbackGateway;
    private final ReportStorageGateway reportStorageGateway;
    private final NotificationGateway notificationGateway;

    public GenerateWeeklyReport(FeedbackGateway feedbackGateway, ReportStorageGateway reportStorageGateway,
                                NotificationGateway notificationGateway) {
        this.feedbackGateway = feedbackGateway;
        this.reportStorageGateway = reportStorageGateway;
        this.notificationGateway = notificationGateway;
    }

    /**
     * Aggregates the feedbacks of the seven days before the reference date, stores the report
     * and publishes its pt-BR summary.
     *
     * @param referenceDate the first day after the reported week (for example, the Monday the schedule runs)
     * @return the report and the location where it was stored
     */
    public GeneratedWeeklyReport execute(LocalDate referenceDate) {
        var period = ReportPeriod.weekBefore(referenceDate);
        var feedbacks = period.days().stream()
                .flatMap(day -> this.feedbackGateway.findByDay(day).stream())
                .toList();
        var report = WeeklyReport.from(feedbacks, period);

        var location = this.reportStorageGateway.save(report);
        this.notificationGateway.publish(PtBrMessages.weeklyReportSubject(period),
                PtBrMessages.weeklyReportMessage(report, location));
        return new GeneratedWeeklyReport(report, location);
    }
}
