package com.vitorbetmann.schoolapi.usecases.messages;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.ReportPeriod;
import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.entities.WeeklyReport;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class PtBrMessages {

    public static final String CRITICAL_FEEDBACK_SUBJECT = "[Urgente] Novo feedback crítico recebido";
    public static final String CRITICAL_FEEDBACK_INTRO = "Um novo feedback crítico foi recebido.";

    public static final String DESCRIPTION = "Descrição";
    public static final String URGENCY = "Urgência";
    public static final String SUBMITTED_AT = "Data de envio";
    public static final String AVERAGE_SCORE = "Média das avaliações";
    public static final String COUNT_PER_DAY = "Avaliações por dia";
    public static final String COUNT_PER_URGENCY = "Avaliações por urgência";
    public static final String PERIOD = "Período";
    public static final String TOTAL = "Total de avaliações";
    public static final String FEEDBACKS = "Feedbacks";
    public static final String NO_SCORES = "Nenhuma avaliação no período";
    public static final String NO_FEEDBACKS = "Nenhum feedback no período.";
    public static final String FULL_REPORT = "Relatório completo";

    private static final String WEEKLY_REPORT_SUBJECT_PREFIX = "Relatório semanal de feedbacks — ";
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(Feedback.REPORTING_ZONE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PtBrMessages() {
    }

    public static String weeklyReportSubject(ReportPeriod period) {
        return WEEKLY_REPORT_SUBJECT_PREFIX + period.isoWeek();
    }

    public static String criticalFeedbackMessage(Feedback feedback) {
        return CRITICAL_FEEDBACK_INTRO + "\n\n"
                + DESCRIPTION + ": " + feedback.description() + "\n"
                + URGENCY + ": " + urgencyLabel(feedback.urgency()) + "\n"
                + SUBMITTED_AT + ": " + formatDateTime(feedback.submittedAt()) + "\n";
    }

    public static String weeklyReportMessage(WeeklyReport report, String location) {
        var period = report.period();
        var text = new StringBuilder()
                .append(weeklyReportSubject(period)).append('\n')
                .append(PERIOD).append(": ").append(formatDate(period.start()))
                .append(" a ").append(formatDate(period.end())).append("\n\n")
                .append(AVERAGE_SCORE).append(": ").append(report.averageScore()
                        .map(average -> average.toPlainString().replace('.', ','))
                        .orElse(NO_SCORES)).append('\n')
                .append(TOTAL).append(": ").append(report.items().size()).append("\n\n")
                .append(COUNT_PER_DAY).append(":\n");
        report.countPerDay().forEach((day, count) ->
                text.append("- ").append(formatDate(day)).append(": ").append(count).append('\n'));
        text.append('\n').append(COUNT_PER_URGENCY).append(":\n");
        report.countPerUrgency().forEach((urgency, count) ->
                text.append("- ").append(urgencyLabel(urgency)).append(": ").append(count).append('\n'));
        text.append('\n').append(FEEDBACKS).append(":\n\n");
        if (report.items().isEmpty()) {
            text.append(NO_FEEDBACKS).append("\n\n");
        }
        for (var feedback : report.items()) {
            text.append(DESCRIPTION).append(": ").append(feedback.description()).append('\n')
                    .append(URGENCY).append(": ").append(urgencyLabel(feedback.urgency())).append('\n')
                    .append(SUBMITTED_AT).append(": ").append(formatDateTime(feedback.submittedAt())).append("\n\n");
        }
        return text.append(FULL_REPORT).append(": ").append(location).append('\n').toString();
    }

    public static String urgencyLabel(Urgency urgency) {
        return switch (urgency) {
            case CRITICAL -> "Crítica";
            case MEDIUM -> "Média";
            case LOW -> "Baixa";
        };
    }

    public static String formatDateTime(Instant instant) {
        return DATE_TIME.format(instant);
    }

    public static String formatDate(LocalDate date) {
        return DATE.format(date);
    }
}
