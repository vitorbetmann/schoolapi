package com.vitorbetmann.schoolapi.usecases.messages;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.ReportPeriod;
import com.vitorbetmann.schoolapi.entities.Urgency;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class PtBrMessages {

    public static final String CRITICAL_FEEDBACK_SUBJECT = "[Urgente] Novo feedback crítico recebido";

    public static final String DESCRIPTION = "Descrição";
    public static final String URGENCY = "Urgência";
    public static final String SUBMITTED_AT = "Data de envio";
    public static final String AVERAGE_SCORE = "Média das avaliações";
    public static final String COUNT_PER_DAY = "Avaliações por dia";
    public static final String COUNT_PER_URGENCY = "Avaliações por urgência";

    private static final String WEEKLY_REPORT_SUBJECT_PREFIX = "Relatório semanal de feedbacks — ";
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(Feedback.REPORTING_ZONE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PtBrMessages() {
    }

    public static String weeklyReportSubject(ReportPeriod period) {
        return WEEKLY_REPORT_SUBJECT_PREFIX + period.isoWeek();
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
