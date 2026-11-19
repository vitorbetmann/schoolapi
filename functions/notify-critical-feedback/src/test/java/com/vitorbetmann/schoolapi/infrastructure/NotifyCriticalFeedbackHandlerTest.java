package com.vitorbetmann.schoolapi.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.vitorbetmann.schoolapi.usecases.NotifyCriticalFeedback;
import com.vitorbetmann.schoolapi.usecases.interfaces.NotificationGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotifyCriticalFeedbackHandlerTest {

    private static final String STREAM_EVENT = "../../events/dynamodb-stream-insert.json";

    private final RecordingNotificationGateway notificationGateway = new RecordingNotificationGateway();
    private final NotifyCriticalFeedbackHandler handler =
            new NotifyCriticalFeedbackHandler(new NotifyCriticalFeedback(this.notificationGateway));
    private final Context context = mock(Context.class);

    @Test
    void publishesNoticeForCriticalInsert() {
        var response = this.handler.handleRequest(sampleEvent(), this.context);

        assertThat(response.getBatchItemFailures()).isEmpty();
        assertThat(this.notificationGateway.messages).singleElement().satisfies(message -> assertThat(message)
                .contains("Descrição: A aula ficou confusa e os exemplos não funcionaram.")
                .contains("Urgência: Crítica")
                .contains("Data de envio: 18/11/2026 10:45"));
        assertThat(this.notificationGateway.subjects).containsExactly("[Urgente] Novo feedback crítico recebido");
    }

    @Test
    void reportsFailedRecordBySequenceNumber() {
        this.notificationGateway.failing = true;

        var response = this.handler.handleRequest(sampleEvent(), this.context);

        assertThat(response.getBatchItemFailures()).singleElement()
                .satisfies(failure -> assertThat(failure.getItemIdentifier()).isEqualTo("111"));
    }

    @Test
    void reportsRecordWithoutImageAsFailed() {
        var event = sampleEvent();
        event.getRecords().getFirst().getDynamodb().setNewImage(null);

        var response = this.handler.handleRequest(event, this.context);

        assertThat(response.getBatchItemFailures()).hasSize(1);
        assertThat(this.notificationGateway.messages).isEmpty();
    }

    private static DynamodbEvent sampleEvent() {
        return EventLoader.loadDynamoDbEvent(STREAM_EVENT);
    }

    private static final class RecordingNotificationGateway implements NotificationGateway {
        private final List<String> subjects = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();
        private boolean failing;

        @Override
        public void publish(String subject, String message) {
            if (this.failing) {
                throw new IllegalStateException("SNS unavailable");
            }
            this.subjects.add(subject);
            this.messages.add(message);
        }
    }
}
