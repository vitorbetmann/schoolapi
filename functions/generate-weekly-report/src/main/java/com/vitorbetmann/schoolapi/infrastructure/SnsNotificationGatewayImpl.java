package com.vitorbetmann.schoolapi.infrastructure;

import com.vitorbetmann.schoolapi.usecases.interfaces.NotificationGateway;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.util.Objects;

/**
 * {@link NotificationGateway} that publishes to an SNS topic with email subscriptions.
 */
public class SnsNotificationGatewayImpl implements NotificationGateway {

    private final SnsClient snsClient;
    private final String topicArn;

    public SnsNotificationGatewayImpl(SnsClient snsClient, String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    /**
     * Builds a gateway for the given topic, using the Lambda environment's region and credentials.
     *
     * @param topicArn the topic ARN
     * @return a ready-to-use gateway
     */
    public static SnsNotificationGatewayImpl create(String topicArn) {
        Objects.requireNonNull(topicArn, "Notification topic ARN is required");
        var snsClient = SnsClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
        return new SnsNotificationGatewayImpl(snsClient, topicArn);
    }

    @Override
    public void publish(String subject, String message) {
        this.snsClient.publish(request -> request.topicArn(this.topicArn).subject(subject).message(message));
    }
}
