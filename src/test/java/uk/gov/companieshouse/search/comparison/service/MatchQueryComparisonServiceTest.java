package uk.gov.companieshouse.search.comparison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.search.comparison.config.SearchComparisonProperties;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class MatchQueryComparisonServiceTest {

    private static final String DEFAULT_INDEX = "alpha_search";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper mockObjectMapper;

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
                objectMapper,
                40);
    }

    @Test
    void shouldReturnNoDiscrepanciesWhenBlueAndGreenResultsMatch() {
        String query = "test company";
        int size = 40;

        JsonNode blueResponse = objectMapper.readTree("""
                {"hits": {"total": {"value": 1, "relation": "eq"}, \
                "hits": [{"_id": "123", "_source": {"company_name": "TEST COMPANY"}}]}}"""
        );
        JsonNode greenResponse = objectMapper.readTree("""
                {"hits": {"total": {"value": 1, "relation": "eq"}, \
                "hits": [{"_id": "123", "_source": {"company_name": "TEST COMPANY"}}]}}"""
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

        Event.Detail detail = buildDetailWithIndexes(query, size, DEFAULT_INDEX, DEFAULT_INDEX);


        Map<String, Object> result = service.compare(detail);

        assertNotNull(result);
        assertTrue(result.containsKey("match_query_" + query));
        MatchQueryResult matchResult = (MatchQueryResult) result.get("match_query_" + query);
        assertEquals(1, matchResult.getBlueTotal());
        assertEquals(1, matchResult.getGreenTotal());
        assertEquals(0, matchResult.getTotalDiscrepancies());
        assertTrue(matchResult.getDiscrepancies().isEmpty());
    }

    @Test
    void shouldReturnDiscrepanciesWhenBlueAndGreenResultsDiffer() {
        String query = "test company";
        int size = 40;

        JsonNode blueResponse = objectMapper.readTree("""
                {"hits": {"total": {"value": 1, "relation": "eq"}, \
                "hits": [{"_id": "123", "_source": {"company_name": "TEST COMPANY"}}]}}"""
        );
        JsonNode greenResponse = objectMapper.readTree("""
                {"hits": {"total": {"value": 1, "relation": "eq"}, \
                "hits": [{"_id": "999", "_source": {"company_name": "TEST COMPANY"}}]}}"""
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

        Event.Detail detail = buildDetailWithIndexes(query, size, DEFAULT_INDEX, DEFAULT_INDEX);

        Map<String, Object> result = service.compare(detail);

        assertNotNull(result);
        MatchQueryResult matchResult = (MatchQueryResult) result.get("match_query_" + query);
        assertEquals(1, matchResult.getBlueTotal());
        assertEquals(1, matchResult.getGreenTotal());
        assertEquals(1, matchResult.getTotalDiscrepancies());
        assertEquals(1, matchResult.getDiscrepancies().size());
        assertEquals(0, matchResult.getDiscrepancies().getFirst().getPosition());
        assertEquals("123", matchResult.getDiscrepancies().getFirst().getBlue().getDocId());
        assertEquals("999", matchResult.getDiscrepancies().getFirst().getGreen().getDocId());
    }

    @Test
    void shouldReturnNoDiscrepanciesWhenNoResultsFound() {
        String query = "nonexistent";
        int size = 40;

        JsonNode blueResponse = objectMapper.readTree("""
                {"hits": {"total": {"value": 0, "relation": "eq"}, "hits": []}}"""
        );
        JsonNode greenResponse = objectMapper.readTree("""
                {"hits": {"total": {"value": 0, "relation": "eq"}, "hits": []}}"""
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

        Event.Detail detail = buildDetailWithIndexes(query, size, DEFAULT_INDEX, DEFAULT_INDEX);

        Map<String, Object> result = service.compare(detail);

        assertNotNull(result);
        MatchQueryResult matchResult = (MatchQueryResult) result.get("match_query_" + query);
        assertEquals(0, matchResult.getBlueTotal());
        assertEquals(0, matchResult.getGreenTotal());
        assertEquals(0, matchResult.getTotalDiscrepancies());
        assertTrue(matchResult.getDiscrepancies().isEmpty());
    }

    @Test
    void shouldReturnDiscrepancyWhenBlueHasHitAndGreenHasNoHit() {
        String query = "test company";
        int size = 40;

        JsonNode blueResponse = objectMapper.readTree("""
                {"hits": {"total": {"value": 1, "relation": "eq"}, \
                "hits": [{"_id": "123", "_source": {"company_name": "TEST COMPANY"}}]}}"""
        );
        JsonNode greenResponse = objectMapper.readTree("""
                {"hits": {"total": {"value": 0, "relation": "eq"}, "hits": []}}"""
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

        Event.Detail detail = buildDetailWithIndexes(query, size, DEFAULT_INDEX, DEFAULT_INDEX);

        Map<String, Object> result = service.compare(detail);

        assertNotNull(result);
        MatchQueryResult matchResult = (MatchQueryResult) result.get("match_query_" + query);
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

        Event.Detail detail = buildDetailWithIndexes(query, size, DEFAULT_INDEX, DEFAULT_INDEX);

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class, () -> service.compare(detail));

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

        Event.Detail detail = buildDetailWithIndexes(query, size, DEFAULT_INDEX, DEFAULT_INDEX);

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class, () -> service.compare(detail));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to retrieve search results"));
    }

    @Test
    void shouldThrowSearchComparisonExceptionWhenRequestBodySerialisationFails() {

        MatchQueryComparisonService serviceWithMockMapper = new MatchQueryComparisonService(
                new SearchComparisonProperties(
                        "http://localhost:9200",
                        "http://localhost:9201",
                        "alpha_search"
                ),
                restTemplate,
                mockObjectMapper,
                40);

        var jacksonException = JacksonIOException.construct(new IOException("Serialisation failed"), null);
        when(mockObjectMapper.writeValueAsString(any())).thenThrow(jacksonException);

        //Event.Detail detail = new Event.Detail();
        Event.Detail detail = buildDetailWithIndexes("test company", 40, DEFAULT_INDEX, DEFAULT_INDEX);

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class, () -> serviceWithMockMapper.compare(detail));

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to serialize search request"));
        assertEquals(jacksonException, exception.getCause());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldThrowWhenIndexNameIsMissing() {
        Event.Detail detail = new Event.Detail();
        detail.setQuery("test company");
        detail.setSize(40);
        detail.setIndexName(null);

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> service.compare(detail)
        );

        assertEquals(
                "indexName is required and must contain blue_index_name and green_index_name",
                exception.getMessage()
        );
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldThrowWhenBlueIndexNameIsNull() {

        Event.Detail detail = buildDetailWithIndexes("test company", 40, null, DEFAULT_INDEX);

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> service.compare(detail)
        );

        assertEquals("blue_index_name is required and must not be blank", exception.getMessage());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldThrowWhenBlueIndexNameIsBlank() {
        Event.Detail detail = buildDetailWithIndexes("test company", 40, " ", DEFAULT_INDEX);

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> service.compare(detail)
        );

        assertEquals("blue_index_name is required and must not be blank", exception.getMessage());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldThrowWhenGreenIndexNameIsNull() {
        Event.Detail detail = buildDetailWithIndexes("test company", 40, DEFAULT_INDEX, null);

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> service.compare(detail)
        );

        assertEquals("green_index_name is required and must not be blank", exception.getMessage());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldThrowWhenGreenIndexNameIsBlank() {
        Event.Detail detail = buildDetailWithIndexes("test company", 40, DEFAULT_INDEX, " ");

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> service.compare(detail)
        );

        assertEquals("green_index_name is required and must not be blank", exception.getMessage());
        verifyNoInteractions(restTemplate);
    }

    private Event.Detail buildDetailWithIndexes(String query, int size, String blueIndex, String greenIndex) {
        Event.IndexName eventIndexName = new Event.IndexName();
        eventIndexName.setBlueIndexName(blueIndex);
        eventIndexName.setGreenIndexName(greenIndex);

        Event.Detail eventDetail = new Event.Detail();
        eventDetail.setQuery(query);
        eventDetail.setSize(size);
        eventDetail.setIndexName(eventIndexName);

        return eventDetail;
    }


}
