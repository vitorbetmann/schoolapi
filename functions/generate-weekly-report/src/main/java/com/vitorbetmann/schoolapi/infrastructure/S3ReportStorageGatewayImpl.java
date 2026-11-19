package com.vitorbetmann.schoolapi.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vitorbetmann.schoolapi.entities.WeeklyReport;
import com.vitorbetmann.schoolapi.infrastructure.dtos.WeeklyReportDocument;
import com.vitorbetmann.schoolapi.infrastructure.json.JsonMapper;
import com.vitorbetmann.schoolapi.usecases.interfaces.ReportStorageGateway;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * {@link ReportStorageGateway} that writes each report as a JSON object to an S3 bucket.
 */
public class S3ReportStorageGatewayImpl implements ReportStorageGateway {

    private static final String CONTENT_TYPE = "application/json; charset=utf-8";

    private final S3Client s3Client;
    private final String bucketName;

    public S3ReportStorageGatewayImpl(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Builds a gateway for the given bucket, using the Lambda environment's region and credentials.
     *
     * @param bucketName the reports bucket name
     * @return a ready-to-use gateway
     */
    public static S3ReportStorageGatewayImpl create(String bucketName) {
        Objects.requireNonNull(bucketName, "Reports bucket name is required");
        var s3Client = S3Client.builder().httpClient(UrlConnectionHttpClient.create()).build();
        return new S3ReportStorageGatewayImpl(s3Client, bucketName);
    }

    /**
     * Writes the report to {@code reports/YYYY/Www.json}, replacing the previous version for that week.
     *
     * @return the {@code s3://} URI of the stored report
     */
    @Override
    public String save(WeeklyReport report) {
        var key = keyFor(report);
        var request = PutObjectRequest.builder().bucket(this.bucketName).key(key).contentType(CONTENT_TYPE).build();
        this.s3Client.putObject(request, RequestBody.fromString(toJson(report), StandardCharsets.UTF_8));
        return "s3://" + this.bucketName + "/" + key;
    }

    static String keyFor(WeeklyReport report) {
        var isoWeek = report.period().isoWeek();
        return "reports/" + isoWeek.replace('-', '/') + ".json";
    }

    private static String toJson(WeeklyReport report) {
        try {
            return JsonMapper.instance().writeValueAsString(WeeklyReportDocument.fromDomain(report));
        } catch (JsonProcessingException exception) {
            throw new UncheckedIOException("Weekly report serialization failed: " + report.period().isoWeek(),
                    exception);
        }
    }
}
