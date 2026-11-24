package uk.gov.companieshouse.search.comparison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.search.comparison.config.SearchComparisonProperties;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.CountResponse;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class DocumentCountServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private DocumentCountService client;

    @BeforeEach
    void setUp() {
        client = new DocumentCountService(
            new SearchComparisonProperties(
                "http://localhost:9200",
                "http://localhost:9201",
                "alpha_search"
            ),
            restTemplate
        );
    }

    @Test
    void testGetDocumentCounts() {
        Event.Detail detail = buildDetailWithIndexes("alpha_search", "alpha_search");
        var blueCountResponse = new CountResponse(1234);
        var greenCountResponse = new CountResponse(5678);

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(CountResponse.class)))
            .thenReturn(ResponseEntity.ok(blueCountResponse));

        when(restTemplate.exchange(eq("http://localhost:9201/alpha_search/_count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(CountResponse.class)))
            .thenReturn(ResponseEntity.ok(greenCountResponse));

        DocumentCountResponse response = client.getDocumentCounts(detail);

        assertNotNull(response);
        assertEquals(1234, response.totalDocuments().blue());
        assertEquals(5678, response.totalDocuments().green());
     }

    @Test
    void testGetDocumentCountsThrowsWhenClusterReturnsNonOkStatus() {
        Event.Detail detail = buildDetailWithIndexes("alpha_search", "alpha_search");

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(CountResponse.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());

        SearchComparisonException exception = assertThrows(
            SearchComparisonException.class, () -> client.getDocumentCounts(detail));

        assertTrue(exception.getMessage().contains("blue"));
    }

    @Test
    void testGetDocumentCountsThrowsWhenClusterReturnsNullBody() {
        Event.Detail detail = buildDetailWithIndexes("alpha_search", "alpha_search");

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(CountResponse.class)))
            .thenReturn(ResponseEntity.ok(null));

        SearchComparisonException exception = assertThrows(
            SearchComparisonException.class, () -> client.getDocumentCounts(detail));

        assertTrue(exception.getMessage().contains("blue"));
    }

    @Test
    void testGetDocumentCountsThrowsWhenRestClientFails() {
        Event.Detail detail = buildDetailWithIndexes("alpha_search", "alpha_search");

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(CountResponse.class)))
            .thenThrow(new ResourceAccessException("Connection refused"));

        SearchComparisonException exception = assertThrows(
            SearchComparisonException.class, () -> client.getDocumentCounts(detail));

        assertNotNull(exception.getCause());
    }


    @Test
    void testGetDocumentCountsThrowsWhenIndexNameIsMissing() {
        Event.Detail detail = new Event.Detail();

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> client.getDocumentCounts(detail)
        );

        assertEquals(
                "indexName is required and must contain blueIndexName and greenIndexName",
                exception.getMessage()
        );
        verifyNoInteractions(restTemplate);
    }

    @ParameterizedTest
    @MethodSource("invalidIndexNameCases")
    void testGetDocumentCountsThrowsWhenIndexNameIsNullOrBlank(
            String blueIndex,
            String greenIndex,
            String expectedMessage) {
        Event.Detail detail = buildDetailWithIndexes(blueIndex, greenIndex);

        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> client.getDocumentCounts(detail)
        );

        assertEquals(expectedMessage, exception.getMessage());
        verifyNoInteractions(restTemplate);
    }

    private static Stream<Arguments> invalidIndexNameCases() {
        return Stream.of(
                Arguments.of(null, "alpha_search", "blueIndexName is required and must not be blank"),
                Arguments.of(" ", "alpha_search", "blueIndexName is required and must not be blank"),
                Arguments.of("alpha_search", null, "greenIndexName is required and must not be blank"),
                Arguments.of("alpha_search", " ", "greenIndexName is required and must not be blank")
        );
    }

    private Event.Detail buildDetailWithIndexes(String blueIndex, String greenIndex) {
        Event.IndexName indexName = new Event.IndexName();
        indexName.setBlueIndexName(blueIndex);
        indexName.setGreenIndexName(greenIndex);

        Event.Detail detail = new Event.Detail();
        detail.setIndexName(indexName);
        return detail;
    }
}


