package uk.gov.companieshouse.search.comparison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.model.CountResponse;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;

@ExtendWith(MockitoExtension.class)
class DocumentCountServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private DocumentCountService client;

    @BeforeEach
    void setUp() {
        client = new DocumentCountService(
            "http://localhost:9200",
            "http://localhost:9201",
            "alpha_search",
            restTemplate
        );
    }

    @Test
    void testGetDocumentCounts() {
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

        DocumentCountResponse response = client.getDocumentCounts();

        assertNotNull(response);
        assertEquals(1234, response.totalDocuments().blue());
        assertEquals(5678, response.totalDocuments().green());
    }

    @Test
    void testGetDocumentCountsThrowsWhenClusterReturnsNonOkStatus() {
        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(CountResponse.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());

        SearchComparisonException exception = assertThrows(
            SearchComparisonException.class, () -> client.getDocumentCounts());

        assertTrue(exception.getMessage().contains("blue"));
    }

    @Test
    void testGetDocumentCountsThrowsWhenClusterReturnsNullBody() {
        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(CountResponse.class)))
            .thenReturn(ResponseEntity.ok(null));

        SearchComparisonException exception = assertThrows(
            SearchComparisonException.class, () -> client.getDocumentCounts());

        assertTrue(exception.getMessage().contains("blue"));
    }

    @Test
    void testGetDocumentCountsThrowsWhenRestClientFails() {
        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(CountResponse.class)))
            .thenThrow(new ResourceAccessException("Connection refused"));

        SearchComparisonException exception = assertThrows(
            SearchComparisonException.class, () -> client.getDocumentCounts());

        assertNotNull(exception.getCause());
    }

}

