package com.vitorbetmann.schoolapi.usecases;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.usecases.interfaces.NotificationGateway;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotifyCriticalFeedbackTest {

    private final RecordingNotificationGateway notificationGateway = new RecordingNotificationGateway();
    private final NotifyCriticalFeedback notifyCriticalFeedback = new NotifyCriticalFeedback(this.notificationGateway);

    @Test
    void publishesUrgentNoticeInPortuguese() {
        var sent = this.notifyCriticalFeedback.execute(feedback(2, Urgency.CRITICAL));

        assertThat(sent).isTrue();
        assertThat(this.notificationGateway.published).singleElement().satisfies(notice -> {
            assertThat(notice.subject()).isEqualTo("[Urgente] Novo feedback crítico recebido");
            assertThat(notice.message())
                    .contains("Descrição: Aula confusa")
                    .contains("Urgência: Crítica")
                    .contains("Data de envio: 18/11/2026 10:45");
        });
    }

    @Test
    void ignoresFeedbackThatIsNotCritical() {
        var sent = this.notifyCriticalFeedback.execute(feedback(5, Urgency.MEDIUM));

        assertThat(sent).isFalse();
        assertThat(this.notificationGateway.published).isEmpty();
    }

    private static Feedback feedback(int score, Urgency urgency) {
        return new Feedback("3f6c1b2e", "Aula confusa", score, urgency,
                Instant.parse("2026-11-18T13:45:00Z"), LocalDate.parse("2026-11-18"));
    }

    private record Notice(String subject, String message) {
    }

    private static final class RecordingNotificationGateway implements NotificationGateway {
        private final List<Notice> published = new ArrayList<>();

        @Override
        public void publish(String subject, String message) {
            this.published.add(new Notice(subject, message));
        }
    }
}
