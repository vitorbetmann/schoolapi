package com.vitorbetmann.schoolapi.infrastructure.dtos;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.infrastructure.dynamo.FeedbackItem;

import java.util.Map;
import java.util.stream.Collectors;

public final class FeedbackStreamImage {

    private FeedbackStreamImage() {
    }

    public static Feedback toDomain(Map<String, AttributeValue> newImage) {
        if (newImage == null) {
            throw new IllegalArgumentException("Stream record has no NewImage");
        }
        var attributes = newImage.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toSdkValue(entry.getKey(), entry.getValue())));
        return FeedbackItem.fromAttributes(attributes).toDomain();
    }

    private static software.amazon.awssdk.services.dynamodb.model.AttributeValue toSdkValue(
            String name, AttributeValue value) {
        if (value.getS() != null) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS(value.getS());
        }
        if (value.getN() != null) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN(value.getN());
        }
        throw new IllegalArgumentException("Unsupported stream attribute type for: " + name);
    }
}
