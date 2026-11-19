package com.vitorbetmann.schoolapi.infrastructure.json;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMapperTest {

    record Sample(Instant submittedAt, LocalDate day) {
    }

    private final Sample sample = new Sample(Instant.parse("2026-11-18T13:45:00Z"), LocalDate.parse("2026-11-18"));

    @Test
    void writesDatesAsIsoStrings() throws Exception {
        var json = JsonMapper.instance().writeValueAsString(this.sample);

        assertThat(json).isEqualTo("{\"submittedAt\":\"2026-11-18T13:45:00Z\",\"day\":\"2026-11-18\"}");
    }

    @Test
    void readsIsoStringsBack() throws Exception {
        var json = "{\"submittedAt\":\"2026-11-18T13:45:00Z\",\"day\":\"2026-11-18\"}";

        assertThat(JsonMapper.instance().readValue(json, Sample.class)).isEqualTo(this.sample);
    }
}
