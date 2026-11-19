package com.vitorbetmann.schoolapi.infrastructure.dynamo;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DynamoFeedbackGatewayImplTest {

    @Mock
    private DynamoDbTable<FeedbackItem> table;

    @Test
    void putsTheFeedbackAsAnItem() {
        var feedback = new Feedback("3f6c1b2e", "Aula confusa", 2, Urgency.CRITICAL,
                Instant.parse("2026-11-18T13:45:00Z"), LocalDate.parse("2026-11-18"));

        new DynamoFeedbackGatewayImpl(this.table).save(feedback);

        var item = ArgumentCaptor.forClass(FeedbackItem.class);
        verify(this.table).putItem(item.capture());
        assertThat(item.getValue().toDomain()).isEqualTo(feedback);
    }
}
