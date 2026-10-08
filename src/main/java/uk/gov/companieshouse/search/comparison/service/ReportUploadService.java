package uk.gov.companieshouse.search.comparison.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.client.upload.S3UploadClient;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;

/**
 * Serializes a report to JSON and uploads it to S3 Storage.
 */
public class ReportUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");

    private final S3UploadClient s3UploadClient;
    private final ObjectMapper objectMapper;

    public ReportUploadService(S3UploadClient s3UploadClient, ObjectMapper objectMapper) {
        this.s3UploadClient = s3UploadClient;
        this.objectMapper = objectMapper;
    }

    public void upload(String keyPrefix, Object report) {
        String key = String.format("%s/%s.json", keyPrefix, Instant.now());

        LOGGER.info(String.format("Uploading report to S3 with key: %s", key));

        try {
            String content = objectMapper.writeValueAsString(report);
            s3UploadClient.uploadFile(key, content);
            LOGGER.info(String.format("Successfully uploaded report to S3 with key: %s", key));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialise report for S3 upload: " + e.getMessage(), e);
            throw new SearchComparisonException("Failed to serialise report for S3 upload", e);
        } catch (S3Exception e) {
            LOGGER.error("Failed to upload report to S3: " + e.getMessage(), e);
            throw new SearchComparisonException("Failed to upload report to S3", e);
        }
    }
}
