package com.vitorbetmann.schoolapi.infrastructure.dynamo;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public class FeedbackItem {

    public static final String BY_DAY_INDEX = "by-day";

    static final TableSchema<FeedbackItem> SCHEMA = StaticTableSchema.builder(FeedbackItem.class)
            .newItemSupplier(FeedbackItem::new)
            .addAttribute(String.class, attribute -> attribute.name("id")
                    .getter(FeedbackItem::getId).setter(FeedbackItem::setId)
                    .tags(StaticAttributeTags.primaryPartitionKey()))
            .addAttribute(String.class, attribute -> attribute.name("description")
                    .getter(FeedbackItem::getDescription).setter(FeedbackItem::setDescription))
            .addAttribute(Integer.class, attribute -> attribute.name("score")
                    .getter(FeedbackItem::getScore).setter(FeedbackItem::setScore))
            .addAttribute(String.class, attribute -> attribute.name("urgency")
                    .getter(FeedbackItem::getUrgency).setter(FeedbackItem::setUrgency))
            .addAttribute(String.class, attribute -> attribute.name("submittedAt")
                    .getter(FeedbackItem::getSubmittedAt).setter(FeedbackItem::setSubmittedAt)
                    .tags(StaticAttributeTags.secondarySortKey(BY_DAY_INDEX)))
            .addAttribute(String.class, attribute -> attribute.name("day")
                    .getter(FeedbackItem::getDay).setter(FeedbackItem::setDay)
                    .tags(StaticAttributeTags.secondaryPartitionKey(BY_DAY_INDEX)))
            .build();

    private String id;
    private String description;
    private Integer score;
    private String urgency;
    private String submittedAt;
    private String day;

    public static FeedbackItem fromDomain(Feedback feedback) {
        var item = new FeedbackItem();
        item.setId(feedback.id());
        item.setDescription(feedback.description());
        item.setScore(feedback.score());
        item.setUrgency(feedback.urgency().name());
        item.setSubmittedAt(feedback.submittedAt().toString());
        item.setDay(feedback.day().toString());
        return item;
    }

    public static FeedbackItem fromAttributes(Map<String, AttributeValue> attributes) {
        return SCHEMA.mapToItem(attributes);
    }

    public Feedback toDomain() {
        return new Feedback(this.id, this.description, this.score, Urgency.valueOf(this.urgency),
                Instant.parse(this.submittedAt), LocalDate.parse(this.day));
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getScore() {
        return this.score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getUrgency() {
        return this.urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getSubmittedAt() {
        return this.submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getDay() {
        return this.day;
    }

    public void setDay(String day) {
        this.day = day;
    }
}
