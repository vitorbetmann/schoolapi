package com.vitorbetmann.schoolapi.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse.BatchItemFailure;
import com.vitorbetmann.schoolapi.infrastructure.dtos.FeedbackStreamImage;
import com.vitorbetmann.schoolapi.usecases.NotifyCriticalFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;

import java.util.ArrayList;

public class NotifyCriticalFeedbackHandler implements RequestHandler<DynamodbEvent, StreamsEventResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyCriticalFeedbackHandler.class);

    private final NotifyCriticalFeedback notifyCriticalFeedback;

    public NotifyCriticalFeedbackHandler() {
        this(Wiring.NOTIFY_CRITICAL_FEEDBACK);
    }

    NotifyCriticalFeedbackHandler(NotifyCriticalFeedback notifyCriticalFeedback) {
        this.notifyCriticalFeedback = notifyCriticalFeedback;
    }

    @Override
    public StreamsEventResponse handleRequest(DynamodbEvent event, Context context) {
        return PowertoolsLogging.withLogging(context, () -> notifyEach(event));
    }

    private StreamsEventResponse notifyEach(DynamodbEvent event) {
        var failures = new ArrayList<BatchItemFailure>();
        for (var record : event.getRecords()) {
            var sequenceNumber = record.getDynamodb().getSequenceNumber();
            try {
                var feedback = FeedbackStreamImage.toDomain(record.getDynamodb().getNewImage());
                var sent = this.notifyCriticalFeedback.execute(feedback);
                LOGGER.info("Critical feedback processed id={} noticeSent={}", feedback.id(), sent);
            } catch (RuntimeException exception) {
                LOGGER.error("Critical feedback notice failed sequenceNumber={}", sequenceNumber, exception);
                failures.add(new BatchItemFailure(sequenceNumber));
            }
        }
        return StreamsEventResponse.builder().withBatchItemFailures(failures).build();
    }

    private static final class Wiring {
        static final NotifyCriticalFeedback NOTIFY_CRITICAL_FEEDBACK = new NotifyCriticalFeedback(
                SnsNotificationGatewayImpl.create(System.getenv("TOPIC_ARN")));
    }
}
