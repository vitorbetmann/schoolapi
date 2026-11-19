package com.vitorbetmann.schoolapi.infrastructure.dynamo;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackItemTest {

    private final Feedback feedback = new Feedback("3f6c1b2e", "Aula confusa", 2, Urgency.CRITICAL,
            Instant.parse("2026-11-18T13:45:00.123Z"), LocalDate.parse("2026-11-18"));

    @Test
    void storesDomainValuesAsDynamoAttributes() {
        var item = FeedbackItem.fromDomain(this.feedback);

        assertThat(item.getId()).isEqualTo("3f6c1b2e");
        assertThat(item.getDescription()).isEqualTo("Aula confusa");
        assertThat(item.getScore()).isEqualTo(2);
        assertThat(item.getUrgency()).isEqualTo("CRITICAL");
        assertThat(item.getSubmittedAt()).isEqualTo("2026-11-18T13:45:00.123Z");
        assertThat(item.getDay()).isEqualTo("2026-11-18");
    }

    @Test
    void rebuildsTheSameFeedback() {
        assertThat(FeedbackItem.fromDomain(this.feedback).toDomain()).isEqualTo(this.feedback);
    }

    @Test
    void mapsToEnglishAttributeNames() {
        var attributes = FeedbackItem.SCHEMA.itemToMap(FeedbackItem.fromDomain(this.feedback), true);

        assertThat(attributes).containsOnlyKeys("id", "description", "score", "urgency", "submittedAt", "day");
        assertThat(attributes.get("score").n()).isEqualTo("2");
    }

    @Test
    void rebuildsFeedbackFromStreamAttributes() {
        var attributes = Map.of(
                "id", AttributeValue.fromS("3f6c1b2e"),
                "description", AttributeValue.fromS("Aula confusa"),
                "score", AttributeValue.fromN("2"),
                "urgency", AttributeValue.fromS("CRITICAL"),
                "submittedAt", AttributeValue.fromS("2026-11-18T13:45:00.123Z"),
                "day", AttributeValue.fromS("2026-11-18"));

        assertThat(FeedbackItem.fromAttributes(attributes).toDomain()).isEqualTo(this.feedback);
    }
}
