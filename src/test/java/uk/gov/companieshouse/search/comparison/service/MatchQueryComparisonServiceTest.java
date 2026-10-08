package uk.gov.companieshouse.search.comparison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.search.comparison.config.SearchComparisonProperties;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class MatchQueryComparisonServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private MatchQueryComparisonService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new MatchQueryComparisonService(
                new SearchComparisonProperties(
                        "http://localhost:9200",
                        "http://localhost:9201",
                        "alpha_search"
                ),
                restTemplate,
                objectMapper
        );
    }

    @Test
    void shouldReturnNoDiscrepanciesWhenBlueAndGreenResultsMatch() throws Exception {
        String query = "test company";
        int size = 40;

        JsonNode blueResponse = objectMapper.readTree(
                "{\"hits\": {\"total\": {\"value\": 1, \"relation\": \"eq\"}, " +
                        "\"hits\": [{\"_id\": \"123\", \"_source\": {\"company_name\": \"TEST COMPANY\"}}]}}"
        );
        JsonNode greenResponse = objectMapper.readTree(
                "{\"hits\": {\"total\": {\"value\": 1, \"relation\": \"eq\"}, " +
                        "\"hits\": [{\"_id\": \"123\", \"_source\": {\"company_name\": \"TEST COMPANY\"}}]}}"
        );

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(blueResponse));

        when(restTemplate.exchange(eq("http://localhost:9201/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(greenResponse));

        Map<String, MatchQueryResult> result = service.compare(query, size);

        assertNotNull(result);
        assertTrue(result.containsKey("match_query_" + query));
        MatchQueryResult matchResult = result.get("match_query_" + query);
        assertEquals(1, matchResult.getBlueTotal());
        assertEquals(1, matchResult.getGreenTotal());
        assertEquals(0, matchResult.getTotalDiscrepancies());
        assertTrue(matchResult.getDiscrepancies().isEmpty());
    }

    @Test
    void shouldReturnDiscrepanciesWhenBlueAndGreenResultsDiffer() throws Exception {
        String query = "test company";
        int size = 40;

        JsonNode blueResponse = objectMapper.readTree(
                "{\"hits\": {\"total\": {\"value\": 1, \"relation\": \"eq\"}, " +
                        "\"hits\": [{\"_id\": \"123\", \"_source\": {\"company_name\": \"TEST COMPANY\"}}]}}"
        );
        JsonNode greenResponse = objectMapper.readTree(
                "{\"hits\": {\"total\": {\"value\": 1, \"relation\": \"eq\"}, " +
                        "\"hits\": [{\"_id\": \"999\", \"_source\": {\"company_name\": \"TEST COMPANY\"}}]}}"
        );

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(blueResponse));

        when(restTemplate.exchange(eq("http://localhost:9201/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(greenResponse));

        Map<String, MatchQueryResult> result = service.compare(query, size);

        assertNotNull(result);
        MatchQueryResult matchResult = result.get("match_query_" + query);
        assertEquals(1, matchResult.getBlueTotal());
        assertEquals(1, matchResult.getGreenTotal());
        assertEquals(1, matchResult.getTotalDiscrepancies());
        assertEquals(1, matchResult.getDiscrepancies().size());
        assertEquals(0, matchResult.getDiscrepancies().getFirst().getPosition());
        assertEquals("123", matchResult.getDiscrepancies().getFirst().getBlue().getDocId());
        assertEquals("999", matchResult.getDiscrepancies().getFirst().getGreen().getDocId());
    }

    @Test
    void shouldReturnNoDiscrepanciesWhenNoResultsFound() throws Exception {
        String query = "nonexistent";
        int size = 40;

        JsonNode blueResponse = objectMapper.readTree(
                "{\"hits\": {\"total\": {\"value\": 0, \"relation\": \"eq\"}, \"hits\": []}}"
        );
        JsonNode greenResponse = objectMapper.readTree(
                "{\"hits\": {\"total\": {\"value\": 0, \"relation\": \"eq\"}, \"hits\": []}}"
        );

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(blueResponse));

        when(restTemplate.exchange(eq("http://localhost:9201/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(greenResponse));

        Map<String, MatchQueryResult> result = service.compare(query, size);

        assertNotNull(result);
        MatchQueryResult matchResult = result.get("match_query_" + query);
        assertEquals(0, matchResult.getBlueTotal());
        assertEquals(0, matchResult.getGreenTotal());
        assertEquals(0, matchResult.getTotalDiscrepancies());
        assertTrue(matchResult.getDiscrepancies().isEmpty());
    }

    @Test
    void shouldReturnDiscrepancyWhenBlueHasHitAndGreenHasNoHit() throws Exception {
        String query = "test company";
        int size = 40;

        JsonNode blueResponse = objectMapper.readTree(
                "{\"hits\": {\"total\": {\"value\": 1, \"relation\": \"eq\"}, " +
                        "\"hits\": [{\"_id\": \"123\", \"_source\": {\"company_name\": \"TEST COMPANY\"}}]}}"
        );
        JsonNode greenResponse = objectMapper.readTree(
                "{\"hits\": {\"total\": {\"value\": 0, \"relation\": \"eq\"}, \"hits\": []}}"
        );

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(blueResponse));

        when(restTemplate.exchange(eq("http://localhost:9201/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(greenResponse));

        Map<String, MatchQueryResult> result = service.compare(query, size);

        assertNotNull(result);
        MatchQueryResult matchResult = result.get("match_query_" + query);
        assertEquals(1, matchResult.getBlueTotal());
        assertEquals(0, matchResult.getGreenTotal());
        assertEquals(1, matchResult.getTotalDiscrepancies());
        assertEquals(1, matchResult.getDiscrepancies().size());
        assertEquals(0, matchResult.getDiscrepancies().getFirst().getPosition());
        assertNotNull(matchResult.getDiscrepancies().getFirst().getBlue());
        assertEquals("123", matchResult.getDiscrepancies().getFirst().getBlue().getDocId());
        assertNull(matchResult.getDiscrepancies().getFirst().getGreen());
    }

    @Test
    void shouldThrowExceptionWhenBlueClusterReturnsNonOkStatus() {
        String query = "test company";
        int size = 40;

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class, () -> service.compare(query, size));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to retrieve search results"));
    }

    @Test
    void shouldThrowExceptionWhenBlueClusterReturnsNullBody() {
        String query = "test company";
        int size = 40;

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_search?pretty"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(null));

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class, () -> service.compare(query, size));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to retrieve search results"));
    }


}
