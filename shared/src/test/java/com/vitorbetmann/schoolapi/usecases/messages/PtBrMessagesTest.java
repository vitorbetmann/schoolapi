package com.vitorbetmann.schoolapi.usecases.messages;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.ReportPeriod;
import com.vitorbetmann.schoolapi.entities.Urgency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PtBrMessagesTest {

    @ParameterizedTest
    @CsvSource({"CRITICAL,Crítica", "MEDIUM,Média", "LOW,Baixa"})
    void labelsUrgencyInPortuguese(Urgency urgency, String label) {
        assertThat(PtBrMessages.urgencyLabel(urgency)).isEqualTo(label);
    }

    @Test
    void formatsDateTimeInSaoPauloTime() {
        assertThat(PtBrMessages.formatDateTime(Instant.parse("2026-11-18T13:45:00Z"))).isEqualTo("18/11/2026 10:45");
    }

    @Test
    void formatsDate() {
        assertThat(PtBrMessages.formatDate(LocalDate.parse("2026-11-18"))).isEqualTo("18/11/2026");
    }

    @Test
    void namesWeeklyReportSubjectAfterIsoWeek() {
        var period = ReportPeriod.weekBefore(LocalDate.parse("2026-11-23"));

        assertThat(PtBrMessages.weeklyReportSubject(period)).isEqualTo("Relatório semanal de feedbacks — 2026-W47");
    }

    @Test
    void describesCriticalFeedbackWithLabelsAndSaoPauloTime() {
        var feedback = new Feedback("3f6c1b2e", "Aula confusa", 2, Urgency.CRITICAL,
                Instant.parse("2026-11-18T13:45:00Z"), LocalDate.parse("2026-11-18"));

        assertThat(PtBrMessages.criticalFeedbackMessage(feedback)).isEqualTo("""
                Um novo feedback crítico foi recebido.

                Descrição: Aula confusa
                Urgência: Crítica
                Data de envio: 18/11/2026 10:45
                """);
    }
}
