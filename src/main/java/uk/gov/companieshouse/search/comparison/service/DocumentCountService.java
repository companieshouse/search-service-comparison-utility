package uk.gov.companieshouse.search.comparison.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.SearchResponse;

public class DocumentCountService {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");

    private final String blueSearchClusterUrl;
    private final String greenSearchClusterUrl;
    private final String indexName;
    private final RestTemplate restTemplate;

    public DocumentCountService(String blueSearchClusterUrl, String greenSearchClusterUrl,
                                 String indexName, RestTemplate restTemplate) {
        this.blueSearchClusterUrl = blueSearchClusterUrl;
        this.greenSearchClusterUrl = greenSearchClusterUrl;
        this.indexName = indexName;
        this.restTemplate = restTemplate;
    }

    public DocumentCountResponse getDocumentCounts() {
        LOGGER.info("Fetching document counts from blue and green clusters");

        long blueCount = getDocumentCount(blueSearchClusterUrl, "blue");
        long greenCount = getDocumentCount(greenSearchClusterUrl, "green");

        LOGGER.info(String.format("Blue cluster count: %d, Green cluster count: %d", blueCount, greenCount));

        var counts = new DocumentCountResponse.DocumentCounts(blueCount, greenCount);
        return new DocumentCountResponse(counts);
    }

    private long getDocumentCount(String baseUrl, String clusterName) {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
            .pathSegment(indexName, "_search")
            .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<SearchResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity, SearchResponse.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                LOGGER.error(String.format("Failed to get document count from %s cluster. Status: %s",
                    clusterName, response.getStatusCode()));
                throw new SearchComparisonException("Failed to get document count from " + clusterName);
            }

            long count = response.getBody().hits().total().value();
            LOGGER.info(String.format("Document count from %s cluster: %d", clusterName, count));
            return count;
        } catch (RestClientException e) {
            LOGGER.error(String.format("Error querying %s cluster at %s: %s", clusterName, url, e.getMessage()), e);
            throw new SearchComparisonException(
                String.format("Failed to get document count from %s cluster", clusterName), e);
        }
    }
}

