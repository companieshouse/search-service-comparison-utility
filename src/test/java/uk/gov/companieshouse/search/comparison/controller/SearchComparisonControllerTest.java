package uk.gov.companieshouse.search.comparison.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import uk.gov.companieshouse.search.comparison.service.DocumentCountService;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;
import uk.gov.companieshouse.search.comparison.model.Discrepancy;
import uk.gov.companieshouse.search.comparison.service.MatchQueryComparisonService;
import uk.gov.companieshouse.search.comparison.service.ComparisonOrchestratorService;
import uk.gov.companieshouse.search.comparison.handler.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@WebMvcTest(SearchComparisonController.class)
class SearchComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentCountService documentCountService;
    @MockitoBean
    private MatchQueryComparisonService matchQueryComparisonService;
    @MockitoBean
    private ComparisonOrchestratorService comparisonOrchestratorService;

    @Test
    void testGetDocumentCount() throws Exception {
        var counts = new DocumentCountResponse.DocumentCounts(998, 999);
        var response = new DocumentCountResponse(counts);

        when(documentCountService.getDocumentCounts()).thenReturn(response);

        mockMvc.perform(get("/documents/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_documents.blue", is(998)))
                .andExpect(jsonPath("$.total_documents.green", is(999)));
    }

    @Test
    void testCompareMatchQuery() throws Exception {
        String query = "TESTCOMPANY";
        int size = 40;

        List<Discrepancy> discrepancies = new ArrayList<>();

        var matchResult = new MatchQueryResult();
        matchResult.setBlueTotal(1);
        matchResult.setGreenTotal(1);
        matchResult.setTotalDiscrepancies(0);
        matchResult.setDiscrepancies(discrepancies);

        var response = java.util.Collections.singletonMap("match_query_" + query, matchResult);

        when(matchQueryComparisonService.compare(query, size)).thenReturn(response);

        mockMvc.perform(get("/documents/match-query")
                        .param("query", query)
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match_query_" + query + ".blue_total", is(1)))
                .andExpect(jsonPath("$.match_query_" + query + ".green_total", is(1)))
                .andExpect(jsonPath("$.match_query_" + query + ".total_discrepancies", is(0)));
    }

    @Test
    void testCompareMatchQueryWithDefaultParameters() throws Exception {
        String query = "GIRLONTHEGROUND";
        int size = 40;

        var matchResult = new MatchQueryResult();
        matchResult.setBlueTotal(1);
        matchResult.setGreenTotal(1);
        matchResult.setTotalDiscrepancies(0);
        matchResult.setDiscrepancies(new java.util.ArrayList<>());

        var response = java.util.Collections.singletonMap("match_query_" + query, matchResult);

        when(matchQueryComparisonService.compare(query, size)).thenReturn(response);

        mockMvc.perform(get("/documents/match-query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match_query_" + query + ".blue_total", is(1)))
                .andExpect(jsonPath("$.match_query_" + query + ".green_total", is(1)))
                .andExpect(jsonPath("$.match_query_" + query + ".total_discrepancies", is(0)));
    }

    @Test
    void testCompareDocuments() throws Exception {
        String requestBody = """
                {
                "details": [
                {
                "operation": "MATCH_QUERY",
                "query": "TESTCOMPANY",
                "size":40 }
                ]
                }
                """;

        Map<String, Object> response = Map.of(
                "MATCH_QUERY", Map.of("status", "ok")
        );

        when(comparisonOrchestratorService.orchestrate(any(Event.class)))
                .thenReturn(response);

        mockMvc.perform(post("/documents/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MATCH_QUERY.status", is("ok")));

        verify(comparisonOrchestratorService).orchestrate(any(Event.class));
    }

    @Test
    void testCompareDocumentsReturns500OnException() throws Exception {
        String requestBody = """
                {
                "details": [
                {
                "operation": "MATCH_QUERY",
                "query": "TESTCOMPANY",
                "size":40 }
                ]
                }
                """;

        when(comparisonOrchestratorService.orchestrate(any(Event.class)))
                .thenThrow(new RuntimeException("failure"));

        mockMvc.perform(post("/documents/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError());
    }

}

