package com.vitorbetmann.schoolapi.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitorbetmann.schoolapi.infrastructure.dtos.ProblemResponse;
import com.vitorbetmann.schoolapi.infrastructure.dtos.SubmitFeedbackRequest;
import com.vitorbetmann.schoolapi.infrastructure.dtos.SubmitFeedbackResponse;
import com.vitorbetmann.schoolapi.infrastructure.dynamo.DynamoFeedbackGatewayImpl;
import com.vitorbetmann.schoolapi.infrastructure.json.JsonMapper;
import com.vitorbetmann.schoolapi.usecases.SubmitFeedback;
import com.vitorbetmann.schoolapi.usecases.exceptions.InvalidFeedbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.logging.CorrelationIdPaths;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;

import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;

public class SubmitFeedbackHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitFeedbackHandler.class);
    private static final ObjectMapper JSON = JsonMapper.instance();
    private static final Map<String, String> JSON_HEADERS = Map.of("Content-Type", "application/json");
    private static final Map<String, String> PROBLEM_HEADERS = Map.of("Content-Type", "application/problem+json");

    private final SubmitFeedback submitFeedback;

    public SubmitFeedbackHandler() {
        this(Wiring.SUBMIT_FEEDBACK);
    }

    SubmitFeedbackHandler(SubmitFeedback submitFeedback) {
        this.submitFeedback = submitFeedback;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        return PowertoolsLogging.withLogging(context, CorrelationIdPaths.API_GATEWAY_REST, event,
                () -> submit(event));
    }

    private APIGatewayProxyResponseEvent submit(APIGatewayProxyRequestEvent event) {
        try {
            var request = parse(event.getBody());
            var feedback = this.submitFeedback.execute(request.description(), request.score());
            LOGGER.info("Feedback stored id={} score={} urgency={}",
                    feedback.id(), feedback.score(), feedback.urgency());
            return respond(201, JSON_HEADERS, SubmitFeedbackResponse.fromDomain(feedback));
        } catch (InvalidFeedbackException exception) {
            LOGGER.info("Feedback rejected violations={}", exception.violations());
            return respond(400, PROBLEM_HEADERS, ProblemResponse.invalidFeedback(exception.violations()));
        } catch (RuntimeException exception) {
            LOGGER.error("Feedback submission failed", exception);
            return respond(500, PROBLEM_HEADERS, ProblemResponse.internalError());
        }
    }

    private static SubmitFeedbackRequest parse(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidFeedbackException(List.of("Request body is required"));
        }
        try {
            var request = JSON.readValue(body, SubmitFeedbackRequest.class);
            if (request == null) {
                throw new InvalidFeedbackException(List.of("Request body is required"));
            }
            return request;
        } catch (JsonProcessingException exception) {
            throw new InvalidFeedbackException(List.of("Request body is malformed"));
        }
    }

    private static APIGatewayProxyResponseEvent respond(int status, Map<String, String> headers, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(headers)
                    .withBody(JSON.writeValueAsString(body));
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static final class Wiring {
        static final SubmitFeedback SUBMIT_FEEDBACK = new SubmitFeedback(
                DynamoFeedbackGatewayImpl.create(System.getenv("TABLE_NAME")), Clock.systemUTC());
    }
}
