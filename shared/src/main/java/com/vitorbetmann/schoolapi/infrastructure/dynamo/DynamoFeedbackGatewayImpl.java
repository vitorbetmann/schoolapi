package com.vitorbetmann.schoolapi.infrastructure.dynamo;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.usecases.interfaces.FeedbackGateway;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * {@link FeedbackGateway} backed by a DynamoDB table with a {@value FeedbackItem#BY_DAY_INDEX} index.
 */
public class DynamoFeedbackGatewayImpl implements FeedbackGateway {

    private final DynamoDbTable<FeedbackItem> table;

    public DynamoFeedbackGatewayImpl(DynamoDbTable<FeedbackItem> table) {
        this.table = table;
    }

    /**
     * Builds a gateway for the given table, using the Lambda environment's region and credentials.
     *
     * @param tableName the feedback table name
     * @return a ready-to-use gateway
     */
    public static DynamoFeedbackGatewayImpl create(String tableName) {
        Objects.requireNonNull(tableName, "Feedback table name is required");
        var dynamoDbClient = DynamoDbClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
        var enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        return new DynamoFeedbackGatewayImpl(enhancedClient.table(tableName, FeedbackItem.SCHEMA));
    }

    @Override
    public void save(Feedback feedback) {
        this.table.putItem(FeedbackItem.fromDomain(feedback));
    }

    @Override
    public List<Feedback> findByDay(LocalDate day) {
        var sameDay = QueryConditional.keyEqualTo(Key.builder().partitionValue(day.toString()).build());
        return this.table.index(FeedbackItem.BY_DAY_INDEX).query(sameDay).stream()
                .flatMap(page -> page.items().stream())
                .map(FeedbackItem::toDomain)
                .toList();
    }
}
