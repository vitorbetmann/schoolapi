package com.vitorbetmann.schoolapi.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.vitorbetmann.schoolapi.entities.Feedback;
import com.vitorbetmann.schoolapi.entities.Urgency;
import com.vitorbetmann.schoolapi.entities.WeeklyReport;
import com.vitorbetmann.schoolapi.infrastructure.dtos.GenerateWeeklyReportResponse;
import com.vitorbetmann.schoolapi.infrastructure.json.JsonMapper;
import com.vitorbetmann.schoolapi.usecases.GenerateWeeklyReport;
import com.vitorbetmann.schoolapi.usecases.interfaces.FeedbackGateway;
import com.vitorbetmann.schoolapi.usecases.interfaces.NotificationGateway;
import com.vitorbetmann.schoolapi.usecases.interfaces.ReportStorageGateway;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GenerateWeeklyReportHandlerTest {

    private static final Path MANUAL_EVENT = Path.of("../../events/manual-weekly-report.json");
    private static final Path SCHEDULER_EVENT = Path.of("../../events/scheduler-weekly-report.json");
    // Monday 2026-11-23 01:30 UTC is still Sunday 2026-11-22 in São Paulo.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T01:30:00Z"), ZoneOffset.UTC);

    private final List<LocalDate> queriedDays = new ArrayList<>();
    private final List<WeeklyReport> storedReports = new ArrayList<>();
    private final List<String> subjects = new ArrayList<>();
    private final GenerateWeeklyReportHandler handler = new GenerateWeeklyReportHandler(
            new GenerateWeeklyReport(feedbackGateway(), storageGateway(), notificationGateway()), CLOCK);
    private final Context context = mock(Context.class);

    @Test
    void generatesReportForTheWeekBeforeTheManualReferenceDate() throws IOException {
        var response = invoke(Files.newInputStream(MANUAL_EVENT));

        assertThat(this.queriedDays).first().isEqualTo(LocalDate.parse("2026-11-09"));
        assertThat(this.queriedDays).last().isEqualTo(LocalDate.parse("2026-11-15"));
        assertThat(response).isEqualTo(
                new GenerateWeeklyReportResponse("2026-W46", "s3://reports/reports/2026/W46.json", 1));
        assertThat(this.subjects).containsExactly("Relatório semanal de feedbacks — 2026-W46");
    }

    @Test
    void usesTodayInSaoPauloForScheduledRuns() throws IOException {
        var response = invoke(Files.newInputStream(SCHEDULER_EVENT));

        assertThat(this.queriedDays).first().isEqualTo(LocalDate.parse("2026-11-15"));
        assertThat(this.queriedDays).last().isEqualTo(LocalDate.parse("2026-11-21"));
        assertThat(response.isoWeek()).isEqualTo("2026-W46");
    }

    @Test
    void rejectsMalformedReferenceDate() {
        var input = new ByteArrayInputStream("{\"referenceDate\":\"16/11/2026\"}".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> invoke(input)).isInstanceOf(RuntimeException.class);
        assertThat(this.storedReports).isEmpty();
    }

    private GenerateWeeklyReportResponse invoke(InputStream input) throws IOException {
        var output = new ByteArrayOutputStream();
        this.handler.handleRequest(input, output, this.context);
        return JsonMapper.instance().readValue(output.toByteArray(), GenerateWeeklyReportResponse.class);
    }

    private FeedbackGateway feedbackGateway() {
        return new FeedbackGateway() {
            @Override
            public void save(Feedback feedback) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Feedback> findByDay(LocalDate day) {
                queriedDays.add(day);
                if (!day.equals(LocalDate.parse("2026-11-10"))) {
                    return List.of();
                }
                return List.of(new Feedback("a", "Aula confusa", 2, Urgency.CRITICAL,
                        Instant.parse("2026-11-10T13:00:00Z"), day));
            }
        };
    }

    private ReportStorageGateway storageGateway() {
        return report -> {
            storedReports.add(report);
            return "s3://reports/" + S3ReportStorageGatewayImpl.keyFor(report);
        };
    }

    private NotificationGateway notificationGateway() {
        return (subject, message) -> subjects.add(subject);
    }
}
