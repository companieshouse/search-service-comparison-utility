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
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.SearchResponse;

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
        var blueSearchResponse = new SearchResponse(new SearchResponse.Hits(new SearchResponse.Total(1234, "eq")));
        var greenSearchResponse = new SearchResponse(new SearchResponse.Hits(new SearchResponse.Total(5678, "eq")));

        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_search"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(SearchResponse.class)))
            .thenReturn(ResponseEntity.ok(blueSearchResponse));

        when(restTemplate.exchange(eq("http://localhost:9201/alpha_search/_search"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(SearchResponse.class)))
            .thenReturn(ResponseEntity.ok(greenSearchResponse));

        DocumentCountResponse response = client.getDocumentCounts();

        assertNotNull(response);
        assertEquals(1234, response.totalDocuments().blue());
        assertEquals(5678, response.totalDocuments().green());
    }

    @Test
    void testGetDocumentCountsThrowsWhenClusterReturnsNonOkStatus() {
        when(restTemplate.exchange(eq("http://localhost:9200/alpha_search/_search"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(SearchResponse.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> client.getDocumentCounts());

        assertTrue(exception.getMessage().contains("blue"));
    }

}

