package com.vitorbetmann.schoolapi.infrastructure.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonMapper {

    private static final ObjectMapper INSTANCE = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private JsonMapper() {
    }

    public static ObjectMapper instance() {
        return INSTANCE;
    }
}
