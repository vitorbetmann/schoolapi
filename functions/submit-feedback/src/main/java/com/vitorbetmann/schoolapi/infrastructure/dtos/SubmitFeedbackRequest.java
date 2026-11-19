package com.vitorbetmann.schoolapi.infrastructure.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubmitFeedbackRequest(
        @JsonProperty("descricao") String description,
        @JsonProperty("nota") Integer score) {
}
