package uk.gov.companieshouse.search.comparison.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;
import uk.gov.companieshouse.search.comparison.model.Discrepancy;
import uk.gov.companieshouse.search.comparison.service.MatchQueryComparisonService;
import uk.gov.companieshouse.search.comparison.service.ComparisonOrchestratorService;

import java.util.ArrayList;
import java.util.HashMap;
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

        Map<String, Object> response = new HashMap<>();
        response.put("match_query_" + query, matchResult);

        var detail = new Event.Detail();
        detail.setQuery(query);
        detail.setOperation("match_query");

        var event = new Event();
        event.setDetails(List.of(detail));

        when(comparisonOrchestratorService.orchestrate(any(Event.class))).thenReturn(response);

        mockMvc.perform(post("/documents/compare")
                        .contentType("application/json")
                        .content("{\"details\":[{\"query\":\"TESTCOMPANY\",\"operation\":\"match_query\"}]}"))
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

        Map<String, Object> response = new HashMap<>();
        response.put("match_query_" + query, matchResult);

        when(comparisonOrchestratorService.orchestrate(any(Event.class))).thenReturn(response);

        mockMvc.perform(post("/documents/compare")
                        .contentType("application/json")
                        .content("{\"details\":[{\"query\":\"GIRLONTHEGROUND\",\"operation\":\"match_query\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match_query_" + query + ".blue_total", is(1)))
                .andExpect(jsonPath("$.match_query_" + query + ".green_total", is(1)))
                .andExpect(jsonPath("$.match_query_" + query + ".total_discrepancies", is(0)));
    }

    @Test
    void testCompareMatchQueryWithDetail() throws Exception {
        String query = "TESTCOMPANY";

        var matchResult = new MatchQueryResult();
        matchResult.setBlueTotal(5);
        matchResult.setGreenTotal(3);
        matchResult.setTotalDiscrepancies(2);
        matchResult.setDiscrepancies(new ArrayList<>());

        Map<String, Object> response = new HashMap<>();
        response.put("match_query_" + query, matchResult);

        when(matchQueryComparisonService.compare(any(Event.Detail.class))).thenReturn(response);

        mockMvc.perform(post("/documents/match-query")
                        .contentType("application/json")
                        .content("{\"query\":\"TESTCOMPANY\",\"operation\":\"match_query\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match_query_" + query + ".blue_total", is(5)))
                .andExpect(jsonPath("$.match_query_" + query + ".green_total", is(3)))
                .andExpect(jsonPath("$.match_query_" + query + ".total_discrepancies", is(2)));
    }

}

