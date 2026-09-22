package uk.gov.companieshouse.search.comparison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.model.Discrepancy;
import uk.gov.companieshouse.search.comparison.model.HitDoc;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class MatchQueryComparisonService {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");
    private static final int MAX_DISCREPANCIES = 10;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String blueBaseUrl;
    private final String greenBaseUrl;

    public MatchQueryComparisonService(@Value("${BLUE_SEARCH_CLUSTER_URL}") String blueBaseUrl,
                                       @Value("${GREEN_SEARCH_CLUSTER_URL}") String greenBaseUrl,
                                       RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.blueBaseUrl = blueBaseUrl;
        this.greenBaseUrl = greenBaseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, MatchQueryResult> compare(String query, int size) throws SearchComparisonException {
        JsonNode blue = runSearch(blueBaseUrl, query, size);
        JsonNode green = runSearch(greenBaseUrl, query, size);

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
        return Collections.singletonMap(key, result);
    }

    private JsonNode runSearch(String baseUrl, String query, int size) throws SearchComparisonException {
        String url = baseUrl + "/alpha_search/_search?pretty";

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

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                LOGGER.error(String.format("Failed to retrieve records from %s. Status: %s", baseUrl, response.getStatusCode()));
                throw new SearchComparisonException("Failed to retrieve search results from " + baseUrl);
            }

            return response.getBody();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LOGGER.error(String.format("Error serializing request body for %s: %s", baseUrl, e.getMessage()), e);
            throw new SearchComparisonException("Failed to serialize search request for " + baseUrl, e);
        } catch (RestClientException e) {
            LOGGER.error(String.format("Error querying cluster at %s: %s", baseUrl, e.getMessage()), e);
            throw new SearchComparisonException("Failed to retrieve search results from " + baseUrl, e);
        }

    }

    private List<HitDoc> extractHits(JsonNode root) {
        JsonNode hitsArray = root.path("hits").path("hits");
        if (!hitsArray.isArray()) {
            return List.of();
        }

        List<HitDoc> result = new ArrayList<>();
        for (JsonNode hit : hitsArray) {
            String id = hit.path("_id").asText(null);
            JsonNode sourceNode = hit.path("_source");
            Map<String, Object> source = objectMapper.convertValue(sourceNode, new TypeReference<Map<String, Object>>() {});
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