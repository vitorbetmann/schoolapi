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
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsFactory;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

import java.util.ArrayList;

public class NotifyCriticalFeedbackHandler implements RequestHandler<DynamodbEvent, StreamsEventResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyCriticalFeedbackHandler.class);

    private final NotifyCriticalFeedback notifyCriticalFeedback;
    private final Metrics metrics;

    public NotifyCriticalFeedbackHandler() {
        this(Wiring.NOTIFY_CRITICAL_FEEDBACK, MetricsFactory.getMetricsInstance());
    }

    NotifyCriticalFeedbackHandler(NotifyCriticalFeedback notifyCriticalFeedback, Metrics metrics) {
        this.notifyCriticalFeedback = notifyCriticalFeedback;
        this.metrics = metrics;
    }

    @Override
    public StreamsEventResponse handleRequest(DynamodbEvent event, Context context) {
        return PowertoolsLogging.withLogging(context, () -> {
            try {
                return notifyEach(event);
            } finally {
                this.metrics.flush();
            }
        });
    }

    private StreamsEventResponse notifyEach(DynamodbEvent event) {
        var failures = new ArrayList<BatchItemFailure>();
        var noticesSent = 0;
        for (var record : event.getRecords()) {
            var sequenceNumber = record.getDynamodb().getSequenceNumber();
            try {
                var feedback = FeedbackStreamImage.toDomain(record.getDynamodb().getNewImage());
                var sent = this.notifyCriticalFeedback.execute(feedback);
                LOGGER.info("Critical feedback processed id={} noticeSent={}", feedback.id(), sent);
                if (sent) {
                    noticesSent++;
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Critical feedback notice failed sequenceNumber={}", sequenceNumber, exception);
                failures.add(new BatchItemFailure(sequenceNumber));
            }
        }
        this.metrics.addMetric("NotificationSent", noticesSent, MetricUnit.COUNT);
        return StreamsEventResponse.builder().withBatchItemFailures(failures).build();
    }

    private static final class Wiring {
        static final NotifyCriticalFeedback NOTIFY_CRITICAL_FEEDBACK = new NotifyCriticalFeedback(
                SnsNotificationGatewayImpl.create(System.getenv("TOPIC_ARN")));
    }
}
