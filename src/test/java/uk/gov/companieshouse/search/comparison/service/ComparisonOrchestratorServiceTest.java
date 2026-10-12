package uk.gov.companieshouse.search.comparison.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;

@ExtendWith(MockitoExtension.class)
class ComparisonOrchestratorServiceTest {

    @Mock private DocumentCountService documentCountService;

    @Mock private MatchQueryComparisonService matchQueryComparisonService;

    @Mock private ReportUploadService reportUploadService;

    private ComparisonOrchestratorService service;

    private static final String DEFAULT_QUERY = "default query";
    private static final int DEFAULT_SIZE =25;

    @BeforeEach void setUp() {
        service = new ComparisonOrchestratorService(
                documentCountService,
                matchQueryComparisonService,
                reportUploadService,
                DEFAULT_QUERY,
                DEFAULT_SIZE );
    }

    @Test void shouldReturnErrorWhenEventIsNull() {
        Map<String, Object> result = service.orchestrate(null);
        assertEquals("No details provided in event", result.get("error"));
    }

    @Test void shouldReturnErrorWhenEventDetailsAreEmpty() {
        Event event = mock(Event.class);
        when(event.getDetails()).thenReturn(List.of());

        Map<String, Object> result = service.orchestrate(event);
        assertEquals("No details provided in event", result.get("error"));
    }

    @Test void shouldReturnDocumentCountResultWhenOperationIsDocumentCount() {
        Event event = mock(Event.class);
        Event.Detail detail = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail));
        when(detail.getOperation()).thenReturn("DOCUMENT_COUNT");
        when(detail.getQuery()).thenReturn(null);
        when(detail.getSize()).thenReturn(0);

        DocumentCountResponse response = mock(DocumentCountResponse.class, RETURNS_DEEP_STUBS);
        when(documentCountService.getDocumentCounts()).thenReturn(response);
        when(response.totalDocuments().blue()).thenReturn(10L);
        when(response.totalDocuments().green()).thenReturn(9L);

        Map<String, Object> result = service.orchestrate(event);

        Map<?, ?> opResult = opResult(result, "DOCUMENT_COUNT");
        assertEquals(10L, opResult.get("blue"));
        assertEquals(9L, opResult.get("green"));
    }

    @Test void shouldReturnMatchQueryResultWhenOperationIsMatchQuery() {
        Event event = mock(Event.class);
        Event.Detail detail = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail));
        when(detail.getOperation()).thenReturn("MATCH_QUERY");
        when(detail.getQuery()).thenReturn("test company");
        when(detail.getSize()).thenReturn(40);

        MatchQueryResult matchQueryResult = mock(MatchQueryResult.class);
        when(matchQueryComparisonService.compare("test company",40))
                .thenReturn(Map.of("match_query_test company", matchQueryResult));

        Map<String, Object> result = service.orchestrate(event);

        Map<?, ?> opResult = opResult(result, "MATCH_QUERY");
        assertTrue(opResult.containsKey("match_query_test company"));
        assertEquals(matchQueryResult, opResult.get("match_query_test company"));
    }

    @Test void shouldReturnMatchQueryErrorWhenCompareReturnsNull() {
        Event event = mock(Event.class);
        Event.Detail detail = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail));
        when(detail.getOperation()).thenReturn("MATCH_QUERY");
        when(detail.getQuery()).thenReturn("test company");
        when(detail.getSize()).thenReturn(40);

        when(matchQueryComparisonService.compare("test company",40)).thenReturn(null);

        Map<String, Object> result = service.orchestrate(event);

        Map<?, ?> opResult = opResult(result, "MATCH_QUERY");
        assertEquals("No match query results", opResult.get("error"));
    }


    @Test void shouldReturnMatchQueryErrorWhenCompareThrowsException() {
        Event event = mock(Event.class);
        Event.Detail detail = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail));
        when(detail.getOperation()).thenReturn("MATCH_QUERY");
        when(detail.getQuery()).thenReturn("test company");
        when(detail.getSize()).thenReturn(40);

        when(matchQueryComparisonService.compare(anyString(), anyInt()))
                .thenThrow(SearchComparisonException.class);

        Map<String, Object> result = service.orchestrate(event);

        Map<?, ?> opResult = opResult(result, "MATCH_QUERY");
        assertEquals("Match query comparison failed", opResult.get("error"));
    }

    @Test void shouldUseDefaultQueryAndSizeWhenMatchQueryInputsAreMissing() {
        Event event = mock(Event.class);
        Event.Detail detail = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail));
        when(detail.getOperation()).thenReturn("MATCH_QUERY");
        when(detail.getQuery()).thenReturn(null);
        when(detail.getSize()).thenReturn(0);

        when(matchQueryComparisonService.compare(DEFAULT_QUERY, DEFAULT_SIZE))
                .thenReturn(Map.of());

        service.orchestrate(event);

        verify(matchQueryComparisonService).compare(DEFAULT_QUERY, DEFAULT_SIZE);
    }

    @Test void shouldReturnUnknownOperationError() {
        Event event = mock(Event.class);
        Event.Detail detail = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail));
        when(detail.getOperation()).thenReturn("UNKNOWN_OP");

        Map<String, Object> result = service.orchestrate(event);

        Map<?, ?> opResult = opResult(result, "UNKNOWN_OP");
        assertEquals("Unknown operation: UNKNOWN_OP", opResult.get("error"));
    }

    private Map<?, ?> opResult(Map<String, Object> result, String operation) {
        Object raw = result.get(operation);
        assertInstanceOf(Map.class, raw);
        return (Map<?, ?>) raw;
    }
}