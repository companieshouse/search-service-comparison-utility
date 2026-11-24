package uk.gov.companieshouse.search.comparison.service;

import org.springframework.web.client.HttpClientErrorException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.config.SearchComparisonProperties;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.Discrepancy;
import uk.gov.companieshouse.search.comparison.model.HitDoc;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.JacksonException;
import uk.gov.companieshouse.search.comparison.util.ValidationUtils;

public class MatchQueryComparisonService implements ComparisonService {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");
    private static final int MAX_DISCREPANCIES = 10;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SearchComparisonProperties searchComparisonProperties;
    private final int defaultSize;

    public MatchQueryComparisonService(SearchComparisonProperties searchComparisonProperties,
                                       RestTemplate restTemplate, ObjectMapper objectMapper,
                                       int defaultSize) {
        this.searchComparisonProperties = searchComparisonProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.defaultSize = defaultSize;
    }

    @Override
    public String getOperation() {
        return "MATCH_QUERY";
    }

    @Override
    public Map<String, Object> compare(Event.Detail detail) throws SearchComparisonException {
        LOGGER.info("Executing MATCH_QUERY comparison strategy");
        // Use default value if not provided in event
        String query = detail.getQuery() != null ? detail.getQuery() : "";
        int size = detail.getSize() > 0 ? detail.getSize() : defaultSize;

        if (detail.getIndexName() == null) {
            LOGGER.error("MATCH_QUERY validation failed: indexName is missing");
            throw new SearchComparisonException(
                    "indexName is required and must contain blueIndexName and greenIndexName");
        }

        String blueIndexName = ValidationUtils.validateIndexName(detail.getIndexName().getBlueIndexName(), "blueIndexName");
        String greenIndexName = ValidationUtils.validateIndexName(detail.getIndexName().getGreenIndexName(), "greenIndexName");

        JsonNode blue = Objects.requireNonNull(
                runSearch(searchComparisonProperties.blueSearchClusterUrl(), blueIndexName, query, size),
                "Blue search result cannot be null");
        JsonNode green = Objects.requireNonNull(
                runSearch(searchComparisonProperties.greenSearchClusterUrl(), greenIndexName, query, size),
                "Green search result cannot be null");

        List<HitDoc> blueHits = extractHits(blue);
        List<HitDoc> greenHits = extractHits(green);

        int blueTotal = blue.path("hits").path("total").path("value").asInt(0);
        int greenTotal = green.path("hits").path("total").path("value").asInt(0);

        List<Discrepancy> allDiscrepancies = compareByPosition(blueHits, greenHits);
        int totalDiscrepancies = allDiscrepancies.size();
        List<Discrepancy> displayedDiscrepancies = allDiscrepancies.stream()
                .limit(MAX_DISCREPANCIES)
                .toList();

        LOGGER.info(String.format(
                "Match query search results for query [%s]: Blue count=%d, Green count=%d, Total discrepancies=%d, Displayed discrepancies=%d",
                query,
                blueTotal,
                greenTotal,
                totalDiscrepancies,
                displayedDiscrepancies.size()));

        for (Discrepancy d : displayedDiscrepancies) {
            LOGGER.info("Discrepancy found at position " + d.getPosition()
                    + ". blue doc_id: " + (d.getBlue() == null ? "null" : d.getBlue().getDocId())
                    + ", green doc_id: "
                    + (d.getGreen() == null ? "null" : d.getGreen().getDocId()));
        }

        MatchQueryResult result = new MatchQueryResult();
        result.setBlueTotal(blueTotal);
        result.setGreenTotal(greenTotal);
        result.setTotalDiscrepancies(totalDiscrepancies);
        result.setDiscrepancies(displayedDiscrepancies);

        String key = "match_query_" + query;
        LOGGER.info("MATCH_QUERY comparison completed successfully");
        return Collections.singletonMap(key, result);
    }

    private JsonNode runSearch(String baseUrl, String indexName, String query, int size) throws SearchComparisonException {
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment(indexName)
                .pathSegment("_search")
                .queryParam("pretty")
                .toUriString();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", size);
        body.put("query", Map.of("match", Map.of("items.ordered_alpha_key", Map.of("query", query))));
        body.put("sort", List.of(Map.of("ordered_alpha_key_with_id", Map.of("order", "asc"))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

            JsonNode responseBody = response.getBody();

            if (response.getStatusCode() != HttpStatus.OK || responseBody == null) {
                LOGGER.error(String.format("Failed to retrieve records from %s. Status: %s", baseUrl, response.getStatusCode()));
                throw new SearchComparisonException("Failed to retrieve search results from " + baseUrl);
            }

            return responseBody;

        } catch (JacksonException e) {
            LOGGER.error(String.format("Error serializing request body for %s: %s", baseUrl, e.getMessage()), e);
            throw new SearchComparisonException("Failed to serialize search request for " + baseUrl, e);
        } catch (HttpClientErrorException.NotFound e) {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.contains("index_not_found_exception")) {
                LOGGER.error(String.format("Index [%s] not found on cluster %s ", indexName, baseUrl));
                throw new SearchComparisonException("Index [" + indexName + "] not found on cluster " + baseUrl, e);
            }
            throw new SearchComparisonException("404 returned from " + baseUrl, e);
        } catch (RestClientException e) {
            LOGGER.error(String.format("Error querying cluster at %s: %s", baseUrl, e.getMessage()), e);
            throw new SearchComparisonException("Failed to retrieve search results from " + baseUrl, e);
        }

    }

    private List<HitDoc> extractHits(JsonNode root) {
        if (root == null) {
            return List.of();
        }

        JsonNode hitsArray = root.path("hits").path("hits");
        if (!hitsArray.isArray()) {
            return List.of();
        }

        List<HitDoc> result = new ArrayList<>();
        for (JsonNode hit : hitsArray) {
            String id = hit.path("_id").asString(null);
            JsonNode sourceNode = hit.path("_source");
            Map<String, Object> source = objectMapper.convertValue(sourceNode, new TypeReference<>() {});
            result.add(new HitDoc(id, source));
        }
        return result;
    }

    private List<Discrepancy> compareByPosition(List<HitDoc> blueHits, List<HitDoc> greenHits) {
        int max = Math.max(blueHits.size(), greenHits.size());
        List<Discrepancy> diffs = new ArrayList<>();

        for (int position = 0; position < max; position++) {
            HitDoc b = position < blueHits.size() ? blueHits.get(position) : null;
            HitDoc g = position < greenHits.size() ? greenHits.get(position) : null;

            String bId = b == null ? null : b.getDocId();
            String gId = g == null ? null : g.getDocId();

            if (!Objects.equals(bId, gId)) {
                diffs.add(new Discrepancy(position, b, g));
            }
        }
        return diffs;
    }
}
