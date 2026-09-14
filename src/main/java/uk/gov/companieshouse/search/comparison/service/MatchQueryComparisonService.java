package uk.gov.companieshouse.search.comparison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.model.Discrepancy;
import uk.gov.companieshouse.search.comparison.model.HitDoc;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;

@Service
public class MatchQueryComparisonService {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String blueBaseUrl;
    private final String greenBaseUrl;


    // @Value("${search.blue.base-url:http://localhost:9400}")
//    @Value("${BLUE_SEARCH_CLUSTER_URL}")
//    private String blueBaseUrl;

    //@Value("${search.green.base-url:http://localhost:9500}")
//    @Value("${GREEN_SEARCH_CLUSTER_URL}")
//    private String greenBaseUrl;

    public MatchQueryComparisonService(@Value("${BLUE_SEARCH_CLUSTER_URL}") String blueBaseUrl,
                                       @Value("${GREEN_SEARCH_CLUSTER_URL}") String greenBaseUrl,
                                       RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.blueBaseUrl = blueBaseUrl;
        this.greenBaseUrl = greenBaseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, MatchQueryResult> compare(String query, int size) throws Exception {
        JsonNode blue = runSearch(blueBaseUrl, query, size);
        JsonNode green = runSearch(greenBaseUrl, query, size);

        List<HitDoc> blueHits = extractHits(blue);
        List<HitDoc> greenHits = extractHits(green);

        int blueTotal = blue.path("hits").path("total").path("value").asInt(0);
        int greenTotal = green.path("hits").path("total").path("value").asInt(0);

        List<Discrepancy> discrepancies = compareByPosition(blueHits, greenHits);

        for (Discrepancy d : discrepancies) {
            LOGGER.info("Discrepancy found. blue doc_id: "
                    + (d.getBlue() == null ? "null" : d.getBlue().getDocId())
                    + ", green doc_id: "
                    + (d.getGreen() == null ? "null" : d.getGreen().getDocId()));
        }

        MatchQueryResult result = new MatchQueryResult();
        result.setBlueTotal(blueTotal);
        result.setGreenTotal(greenTotal);
        result.setDiscrepancies(discrepancies);

        String key = "match_query_" + query;
        return Collections.singletonMap(key, result);
    }

    private JsonNode runSearch(String baseUrl, String query, int size) throws Exception {
        String url = baseUrl + "/alpha_search/_search?pretty";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", size);
        body.put("query", Map.of("match", Map.of("items.ordered_alpha_key", Map.of("query", query))));
        body.put("sort", List.of(Map.of("ordered_alpha_key_with_id", Map.of("order", "asc"))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
        return Objects.requireNonNull(response.getBody());
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
            Map<String, Object> source = objectMapper.convertValue(sourceNode, Map.class);
            result.add(new HitDoc(id, source));
        }
        return result;
    }

    private List<Discrepancy> compareByPosition(List<HitDoc> blueHits, List<HitDoc> greenHits) {
        int max = Math.max(blueHits.size(), greenHits.size());
        List<Discrepancy> diffs = new ArrayList<>();

        for (int i = 0; i < max; i++) {
            HitDoc b = i < blueHits.size() ? blueHits.get(i) : null;
            HitDoc g = i < greenHits.size() ? greenHits.get(i) : null;

            String bId = b == null ? null : b.getDocId();
            String gId = g == null ? null : g.getDocId();

            if (!Objects.equals(bId, gId)) {
                diffs.add(new Discrepancy(b, g));
            }
        }
        return diffs;
    }
}