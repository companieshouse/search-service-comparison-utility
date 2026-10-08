package uk.gov.companieshouse.search.comparison.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.client.upload.S3UploadClient;
import uk.gov.companieshouse.search.comparison.config.SearchComparisonProperties;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.model.CountResponse;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;

public class DocumentCountService {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");
    private static final String S3_KEY_PREFIX = "document-counts";

    private final SearchComparisonProperties searchComparisonProperties;
    private final RestTemplate restTemplate;
    private final S3UploadClient s3UploadClient;
    private final ObjectMapper objectMapper;

    public DocumentCountService(SearchComparisonProperties searchComparisonProperties, RestTemplate restTemplate,
                                 S3UploadClient s3UploadClient, ObjectMapper objectMapper) {
        this.searchComparisonProperties = searchComparisonProperties;
        this.restTemplate = restTemplate;
        this.s3UploadClient = s3UploadClient;
        this.objectMapper = objectMapper;
    }

    public DocumentCountResponse getDocumentCounts() {
        LOGGER.info("Fetching document counts from blue and green clusters");

        long blueCount = getDocumentCount(searchComparisonProperties.blueSearchClusterUrl(), "blue");
        long greenCount = getDocumentCount(searchComparisonProperties.greenSearchClusterUrl(), "green");

        LOGGER.info(String.format("Blue cluster count: %d, Green cluster count: %d", blueCount, greenCount));

        var counts = new DocumentCountResponse.DocumentCounts(blueCount, greenCount);
        var response = new DocumentCountResponse(counts);

        uploadDocumentCounts(response);

        return response;
    }

    private void uploadDocumentCounts(DocumentCountResponse response) {
        String key = String.format("%s/%s.json", S3_KEY_PREFIX, Instant.now());

        LOGGER.info(String.format("Uploading document counts to S3 with key: %s", key));

        try {
            String content = objectMapper.writeValueAsString(response);
            s3UploadClient.uploadFile(key, content);
            LOGGER.info(String.format("Successfully uploaded document counts to S3 with key: %s", key));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialise document counts for S3 upload: " + e.getMessage(), e);
            throw new SearchComparisonException("Failed to serialise document counts for S3 upload", e);
        } catch (S3Exception e) {
            LOGGER.error("Failed to upload document counts to S3: " + e.getMessage(), e);
            throw new SearchComparisonException("Failed to upload document counts to S3", e);
        }
    }

    private long getDocumentCount(String baseUrl, String clusterName) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
            .pathSegment(searchComparisonProperties.indexName(), "_count")
            .toUriString();

        LOGGER.info(String.format("Querying document count for %s cluster using URL: %s", clusterName, url));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CountResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity, CountResponse.class);

            CountResponse body = response.getBody();
            if (response.getStatusCode() != HttpStatus.OK || body == null) {
                LOGGER.error(String.format("Failed to get document count from %s cluster. Status: %s",
                    clusterName, response.getStatusCode()));
                throw new SearchComparisonException("Failed to get document count from " + clusterName);
            }

            long count = body.count();
            LOGGER.info(String.format("Document count from %s cluster: %d", clusterName, count));
            return count;
        } catch (RestClientException e) {
            LOGGER.error(String.format("Error querying %s cluster at %s: %s", clusterName, url, e.getMessage()), e);
            throw new SearchComparisonException(
                String.format("Failed to get document count from %s cluster", clusterName), e);
        }
    }
}

