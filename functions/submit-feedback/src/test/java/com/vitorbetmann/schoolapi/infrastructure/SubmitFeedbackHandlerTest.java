package com.vitorbetmann.schoolapi.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.infrastructure.json.JsonMapper;
import com.vitorbetmann.schoolapi.usecases.SubmitFeedback;
import com.vitorbetmann.schoolapi.usecases.interfaces.FeedbackGateway;
import org.junit.jupiter.api.Test;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SubmitFeedbackHandlerTest {

    private static final Path POST_EVENT = Path.of("../../events/post-avaliacao.json");

    private final RecordingFeedbackGateway feedbackGateway = new RecordingFeedbackGateway();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T13:45:00Z"), ZoneOffset.UTC);
    private final Metrics metrics = mock(Metrics.class);
    private final SubmitFeedbackHandler handler =
            new SubmitFeedbackHandler(new SubmitFeedback(this.feedbackGateway, this.clock), this.metrics);
    private final Context context = mock(Context.class);

    @Test
    void returnsCreatedWithIdUrgencyAndSubmissionTime() throws IOException {
        var response = this.handler.handleRequest(sampleEvent(), this.context);

        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.getHeaders()).containsEntry("Content-Type", "application/json");
        var body = json(response.getBody());
        assertThat(body.get("id").asText()).isEqualTo(this.feedbackGateway.saved.getFirst().id());
        assertThat(body.get("urgency").asText()).isEqualTo("CRITICAL");
        assertThat(body.get("submittedAt").asText()).isEqualTo("2026-11-18T13:45:00Z");
        assertThat(body.properties()).hasSize(3);
    }

    @Test
    void countsReceivedFeedbackByUrgency() throws IOException {
        this.handler.handleRequest(sampleEvent(), this.context);

        verify(this.metrics).addDimension("urgency", "CRITICAL");
        verify(this.metrics).addMetric("FeedbackReceived", 1, MetricUnit.COUNT);
        verify(this.metrics).flush();
    }

    @Test
    void doesNotCountRejectedFeedback() throws IOException {
        this.handler.handleRequest(withBody("{\"descricao\":\"\",\"nota\":11}"), this.context);

        verify(this.metrics, never()).addMetric(anyString(), anyDouble(), any(MetricUnit.class));
        verify(this.metrics).flush();
    }

    @Test
    void storesTheFeedbackFromTheBrazilianContract() throws IOException {
        this.handler.handleRequest(sampleEvent(), this.context);

        var stored = this.feedbackGateway.saved.getFirst();
        assertThat(stored.description()).isEqualTo("A aula ficou confusa e os exemplos não funcionaram.");
        assertThat(stored.score()).isEqualTo(2);
    }

    @Test
    void returnsBadRequestWithEveryViolation() throws IOException {
        var response = this.handler.handleRequest(withBody("{\"descricao\":\"\",\"nota\":11}"), this.context);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getHeaders()).containsEntry("Content-Type", "application/problem+json");
        assertThat(errors(response.getBody())).containsExactly(
                "Feedback description is required",
                "Feedback score out of range: 11");
        assertThat(this.feedbackGateway.saved).isEmpty();
    }

    @Test
    void returnsBadRequestWhenBodyIsMissing() throws IOException {
        var response = this.handler.handleRequest(withBody(null), this.context);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(errors(response.getBody())).containsExactly("Request body is required");
    }

    @Test
    void returnsBadRequestWhenBodyIsNotJson() throws IOException {
        var response = this.handler.handleRequest(withBody("descricao=aula&nota=5"), this.context);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(errors(response.getBody())).containsExactly("Request body is malformed");
    }

    @Test
    void returnsInternalErrorWithoutLeakingDetails() throws IOException {
        this.feedbackGateway.failWith(new IllegalStateException("DynamoDB table Feedbacks not found"));

        var response = this.handler.handleRequest(withBody("{\"descricao\":\"Aula\",\"nota\":5}"), this.context);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(json(response.getBody()).get("title").asText()).isEqualTo("Internal server error");
        assertThat(response.getBody()).doesNotContain("DynamoDB", "Feedbacks");
    }

    private static APIGatewayProxyRequestEvent sampleEvent() throws IOException {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(POST_EVENT.toFile(), APIGatewayProxyRequestEvent.class);
    }

    private static APIGatewayProxyRequestEvent withBody(String body) {
        return new APIGatewayProxyRequestEvent().withHttpMethod("POST").withPath("/avaliacao").withBody(body);
    }

    private static JsonNode json(String body) throws IOException {
        return JsonMapper.instance().readTree(body);
    }

    private static List<String> errors(String body) throws IOException {
        var errors = new ArrayList<String>();
        json(body).get("errors").forEach(error -> errors.add(error.asText()));
        return errors;
    }

    private static final class RecordingFeedbackGateway implements FeedbackGateway {

        private final List<Feedback> saved = new ArrayList<>();
        private RuntimeException failure;

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void save(Feedback feedback) {
            if (this.failure != null) {
                throw this.failure;
            }
            this.saved.add(feedback);
        }

        @Override
        public List<Feedback> findByDay(LocalDate day) {
            return List.of();
        }
    }
}
