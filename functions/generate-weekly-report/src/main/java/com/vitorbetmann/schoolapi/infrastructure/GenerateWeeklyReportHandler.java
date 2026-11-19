package com.vitorbetmann.schoolapi.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.vitorbetmann.schoolapi.infrastructure.dtos.GenerateWeeklyReportRequest;
import com.vitorbetmann.schoolapi.infrastructure.dtos.GenerateWeeklyReportResponse;
import com.vitorbetmann.schoolapi.infrastructure.dynamo.DynamoFeedbackGatewayImpl;
import com.vitorbetmann.schoolapi.infrastructure.json.JsonMapper;
import com.vitorbetmann.schoolapi.usecases.GenerateWeeklyReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsFactory;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Clock;

public class GenerateWeeklyReportHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateWeeklyReportHandler.class);

    private final GenerateWeeklyReport generateWeeklyReport;
    private final Clock clock;
    private final Metrics metrics;

    public GenerateWeeklyReportHandler() {
        this(Wiring.GENERATE_WEEKLY_REPORT, Clock.systemUTC(), MetricsFactory.getMetricsInstance());
    }

    GenerateWeeklyReportHandler(GenerateWeeklyReport generateWeeklyReport, Clock clock, Metrics metrics) {
        this.generateWeeklyReport = generateWeeklyReport;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        PowertoolsLogging.withLogging(context, () -> {
            try {
                generate(input, output);
                return null;
            } finally {
                this.metrics.flush();
            }
        });
    }

    private void generate(InputStream input, OutputStream output) {
        try {
            var mapper = JsonMapper.instance();
            var request = mapper.readValue(input, GenerateWeeklyReportRequest.class);
            var referenceDate = request.referenceDateOr(this.clock);
            var response = GenerateWeeklyReportResponse.fromDomain(this.generateWeeklyReport.execute(referenceDate));
            LOGGER.info("Weekly report generated referenceDate={} isoWeek={} feedbackCount={} location={}",
                    referenceDate, response.isoWeek(), response.feedbackCount(), response.location());
            this.metrics.addMetric("ReportGenerated", 1, MetricUnit.COUNT);
            mapper.writeValue(output, response);
        } catch (IOException exception) {
            throw new UncheckedIOException("Weekly report request could not be read or answered", exception);
        }
    }

    private static final class Wiring {
        static final GenerateWeeklyReport GENERATE_WEEKLY_REPORT = new GenerateWeeklyReport(
                DynamoFeedbackGatewayImpl.create(System.getenv("TABLE_NAME")),
                S3ReportStorageGatewayImpl.create(System.getenv("BUCKET_NAME")),
                SnsNotificationGatewayImpl.create(System.getenv("TOPIC_ARN")));
    }
}
