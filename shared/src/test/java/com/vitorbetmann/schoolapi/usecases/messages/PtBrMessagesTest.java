package com.vitorbetmann.schoolapi.usecases.messages;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.ReportPeriod;
import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.entities.WeeklyReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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

    @Test
    void describesWeeklyReportWithAveragesCountsAndItems() {
        var period = ReportPeriod.weekBefore(LocalDate.parse("2026-11-23"));
        var report = WeeklyReport.from(List.of(
                new Feedback("a", "Aula confusa", 2, Urgency.CRITICAL,
                        Instant.parse("2026-11-16T13:45:00Z"), LocalDate.parse("2026-11-16")),
                new Feedback("b", "Ótima aula", 9, Urgency.LOW,
                        Instant.parse("2026-11-18T01:30:00Z"), LocalDate.parse("2026-11-17"))), period);

        assertThat(PtBrMessages.weeklyReportMessage(report, "s3://bucket/reports/2026/W47.json")).isEqualTo("""
                Relatório semanal de feedbacks — 2026-W47
                Período: 16/11/2026 a 22/11/2026

                Média das avaliações: 5,50
                Total de avaliações: 2

                Avaliações por dia:
                - 16/11/2026: 1
                - 17/11/2026: 1
                - 18/11/2026: 0
                - 19/11/2026: 0
                - 20/11/2026: 0
                - 21/11/2026: 0
                - 22/11/2026: 0

                Avaliações por urgência:
                - Crítica: 1
                - Média: 0
                - Baixa: 1

                Feedbacks:

                Descrição: Aula confusa
                Urgência: Crítica
                Data de envio: 16/11/2026 10:45

                Descrição: Ótima aula
                Urgência: Baixa
                Data de envio: 17/11/2026 22:30

                Relatório completo: s3://bucket/reports/2026/W47.json
                """);
    }

    @Test
    void saysWhenTheWeekHasNoFeedback() {
        var report = WeeklyReport.from(List.of(), ReportPeriod.weekBefore(LocalDate.parse("2026-11-23")));

        assertThat(PtBrMessages.weeklyReportMessage(report, "s3://bucket/key"))
                .contains("Média das avaliações: Nenhuma avaliação no período")
                .contains("Total de avaliações: 0")
                .contains("Feedbacks:\n\nNenhum feedback no período.\n");
    }
}
