package uk.gov.companieshouse.search.comparison.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.search.comparison.handler.Event;


@ExtendWith(MockitoExtension.class)
class ComparisonOrchestratorServiceTest {

    @Mock
    private ComparisonService matchQueryService;

    @Mock
    private ComparisonService documentCountService;

    @Mock
    private ReportUploadService reportUploadService;

    private ComparisonOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        when(matchQueryService.getOperation()).thenReturn("match_query");
        when(documentCountService.getOperation()).thenReturn("document_count");

        orchestratorService = new ComparisonOrchestratorService(
                List.of(matchQueryService, documentCountService),
                reportUploadService,
                "default-query",
                40);
    }

    @Test
    void shouldReturnErrorWhenEventIsNull() {
        Map<String, Object> result = orchestratorService.orchestrate(null);

        assertEquals("No details provided in event", result.get("error"));
        verifyNoInteractions(reportUploadService);
    }

    @Test
    void shouldReturnErrorWhenDetailsAreEmpty() {
        Event event = mock(Event.class);
        when(event.getDetails()).thenReturn(List.of());

        Map<String, Object> result = orchestratorService.orchestrate(event);

        assertEquals("No details provided in event", result.get("error"));
        verifyNoInteractions(reportUploadService);
    }

    @Test
    void shouldRouteToMatchingServicesAndUploadCombinedResults() {
        Event event = mock(Event.class);
        Event.Detail detail1 = mock(Event.Detail.class);
        Event.Detail detail2 = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail1, detail2));
        when(detail1.getOperation()).thenReturn("match_query");
        when(detail2.getOperation()).thenReturn("document_count");

        Map<String, Object> matchResult = Map.of("match_query_test", Map.of("ok", true));
        Map<String, Object> countResult = Map.of("totals", Map.of("blue", 10, "green", 10));

        when(matchQueryService.compare(detail1)).thenReturn(matchResult);
        when(documentCountService.compare(detail2)).thenReturn(countResult);

        Map<String, Object> result = orchestratorService.orchestrate(event);

        assertEquals(matchResult, result.get("match_query"));
        assertEquals(countResult, result.get("document_count"));
        verify(reportUploadService).upload(eq("comparison-report"), eq(result));
    }


    @Test
    void shouldReturnUnknownOperationErrorWhenNoServiceRegistered() {
        Event event = mock(Event.class);
        Event.Detail detail = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail));
        when(detail.getOperation()).thenReturn("unknown_op");

        Map<String, Object> result = orchestratorService.orchestrate(event);

        assertTrue(result.containsKey("unknown_op"));
        Map<?, ?> error = opResult(result, "unknown_op");
        assertEquals("Unknown operation: unknown_op", error.get("error"));
        verify(reportUploadService).upload(eq("comparison-report"), eq(result));
    }


    @Test
    void shouldReturnFailureErrorWhenServiceThrowsException() {
        Event event = mock(Event.class);
        Event.Detail detail = mock(Event.Detail.class);

        when(event.getDetails()).thenReturn(List.of(detail));
        when(detail.getOperation()).thenReturn("match_query");
        when(matchQueryService.compare(detail)).thenThrow(new RuntimeException("test"));

        Map<String, Object> result = orchestratorService.orchestrate(event);

        Map<?, ?> error = opResult(result, "match_query");
        assertEquals("Failed to execute match_query: test", error.get("error"));
        verify(reportUploadService).upload(eq("comparison-report"), eq(result));
    }

    private Map<?, ?> opResult(Map<String, Object> result, String operation) {
        Object raw = result.get(operation);
        assertInstanceOf(Map.class, raw);
        return (Map<?, ?>) raw;
    }

}