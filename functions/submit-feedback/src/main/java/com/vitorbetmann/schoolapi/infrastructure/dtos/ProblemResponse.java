package com.vitorbetmann.schoolapi.infrastructure.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * RFC 9457 (formerly 7807) problem details, with the validation messages in {@code errors}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemResponse(String title, int status, List<String> errors) {

    public static ProblemResponse invalidFeedback(List<String> violations) {
        return new ProblemResponse("Invalid feedback", 400, violations);
    }

    public static ProblemResponse internalError() {
        return new ProblemResponse("Internal server error", 500, null);
    }
}
