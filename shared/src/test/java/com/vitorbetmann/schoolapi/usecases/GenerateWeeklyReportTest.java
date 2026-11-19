package com.vitorbetmann.schoolapi.usecases;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.entities.WeeklyReport;
import com.vitorbetmann.schoolapi.usecases.interfaces.NotificationGateway;
import com.vitorbetmann.schoolapi.usecases.interfaces.ReportStorageGateway;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class GenerateWeeklyReportTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.parse("2026-11-16");
    private static final String LOCATION = "s3://reports/reports/2026/W46.json";

    private final InMemoryFeedbackGateway feedbackGateway = new InMemoryFeedbackGateway();
    private final RecordingReportStorageGateway storageGateway = new RecordingReportStorageGateway();
    private final RecordingNotificationGateway notificationGateway = new RecordingNotificationGateway();
    private final GenerateWeeklyReport generateWeeklyReport =
            new GenerateWeeklyReport(this.feedbackGateway, this.storageGateway, this.notificationGateway);

    @Test
    void aggregatesTheSevenDaysBeforeTheReferenceDate() {
        this.feedbackGateway.save(feedback("a", "2026-11-08", 9));
        this.feedbackGateway.save(feedback("b", "2026-11-09", 2));
        this.feedbackGateway.save(feedback("c", "2026-11-12", 5));
        this.feedbackGateway.save(feedback("d", "2026-11-15", 8));
        this.feedbackGateway.save(feedback("e", "2026-11-16", 1));

        var generated = this.generateWeeklyReport.execute(REFERENCE_DATE);

        var report = generated.report();
        assertThat(report.items()).extracting(Feedback::id).containsExactly("b", "c", "d");
        assertThat(report.averageScore()).contains(new BigDecimal("5.00"));
        assertThat(report.countPerDay()).hasSize(7).contains(
                entry(LocalDate.parse("2026-11-09"), 1L), entry(LocalDate.parse("2026-11-10"), 0L));
        assertThat(report.countPerUrgency()).containsExactly(
                entry(Urgency.CRITICAL, 1L), entry(Urgency.MEDIUM, 1L), entry(Urgency.LOW, 1L));
    }

    @Test
    void storesTheReportAndPublishesSummaryWithItsLocation() {
        this.feedbackGateway.save(feedback("b", "2026-11-09", 2));

        var generated = this.generateWeeklyReport.execute(REFERENCE_DATE);

        assertThat(generated.location()).isEqualTo(LOCATION);
        assertThat(this.storageGateway.saved).containsExactly(generated.report());
        assertThat(this.notificationGateway.subjects).containsExactly("Relatório semanal de feedbacks — 2026-W46");
        assertThat(this.notificationGateway.messages).singleElement().satisfies(message -> assertThat(message)
                .contains("Média das avaliações: 2,00")
                .contains("Urgência: Crítica")
                .contains("Relatório completo: " + LOCATION));
    }

    @Test
    void storesAndPublishesEvenWhenTheWeekHasNoFeedback() {
        var generated = this.generateWeeklyReport.execute(REFERENCE_DATE);

        assertThat(generated.report().items()).isEmpty();
        assertThat(this.storageGateway.saved).hasSize(1);
        assertThat(this.notificationGateway.messages).singleElement()
                .satisfies(message -> assertThat(message).contains("Nenhuma avaliação no período"));
    }

    private static Feedback feedback(String id, String day, int score) {
        return new Feedback(id, "Feedback " + id, score, Urgency.fromScore(score),
                Instant.parse(day + "T13:00:00Z"), LocalDate.parse(day));
    }

    private static final class RecordingReportStorageGateway implements ReportStorageGateway {
        private final List<WeeklyReport> saved = new ArrayList<>();

        @Override
        public String save(WeeklyReport report) {
            this.saved.add(report);
            return LOCATION;
        }
    }

    private static final class RecordingNotificationGateway implements NotificationGateway {
        private final List<String> subjects = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(String subject, String message) {
            this.subjects.add(subject);
            this.messages.add(message);
        }
    }
}
