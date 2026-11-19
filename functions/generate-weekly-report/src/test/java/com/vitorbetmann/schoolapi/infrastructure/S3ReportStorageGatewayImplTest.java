package com.vitorbetmann.schoolapi.infrastructure;

import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.ReportPeriod;
import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.entities.WeeklyReport;
import com.vitorbetmann.schoolapi.infrastructure.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3ReportStorageGatewayImplTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3ReportStorageGatewayImpl gateway = new S3ReportStorageGatewayImpl(this.s3Client, "reports-bucket");

    @Test
    void writesReportAsJsonUnderItsIsoWeek() throws IOException {
        var feedback = new Feedback("a", "Aula confusa", 2, Urgency.CRITICAL,
                Instant.parse("2026-11-10T13:00:00Z"), LocalDate.parse("2026-11-10"));
        var report = WeeklyReport.from(List.of(feedback), ReportPeriod.weekBefore(LocalDate.parse("2026-11-16")));

        var location = this.gateway.save(report);

        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(this.s3Client).putObject(request.capture(), body.capture());
        assertThat(location).isEqualTo("s3://reports-bucket/reports/2026/W46.json");
        assertThat(request.getValue().bucket()).isEqualTo("reports-bucket");
        assertThat(request.getValue().key()).isEqualTo("reports/2026/W46.json");
        assertThat(request.getValue().contentType()).isEqualTo("application/json; charset=utf-8");

        try (var json = body.getValue().contentStreamProvider().newStream()) {
            var document = JsonMapper.instance().readTree(json);
            assertThat(document.get("isoWeek").asText()).isEqualTo("2026-W46");
            assertThat(document.get("averageScore").decimalValue()).isEqualByComparingTo("2.00");
            assertThat(document.get("countPerDay").get("2026-11-10").asLong()).isEqualTo(1);
            assertThat(document.get("countPerUrgency").get("CRITICAL").asLong()).isEqualTo(1);
            assertThat(document.get("items").get(0).get("submittedAt").asText()).isEqualTo("2026-11-10T13:00:00Z");
        }
    }

    @Test
    void writesNullAverageForAnEmptyWeek() throws IOException {
        var report = WeeklyReport.from(List.of(), ReportPeriod.weekBefore(LocalDate.parse("2026-11-16")));

        this.gateway.save(report);

        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(this.s3Client).putObject(any(PutObjectRequest.class), body.capture());
        try (var json = body.getValue().contentStreamProvider().newStream()) {
            var document = JsonMapper.instance().readTree(json);
            assertThat(document.get("averageScore").isNull()).isTrue();
            assertThat(document.get("feedbackCount").asInt()).isZero();
        }
    }
}
