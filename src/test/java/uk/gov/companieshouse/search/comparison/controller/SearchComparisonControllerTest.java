package uk.gov.companieshouse.search.comparison.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;
import uk.gov.companieshouse.search.comparison.model.Discrepancy;
import uk.gov.companieshouse.search.comparison.service.MatchQueryComparisonService;

@WebMvcTest(SearchComparisonController.class)
class SearchComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentCountService documentCountService;
    @MockitoBean
    private MatchQueryComparisonService matchQueryComparisonService;

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
        String query = "GIRLSDAYSCHOOLTRUST";
        int size = 40;

       // var discrepancies = new java.util.ArrayList<>();
        var discrepancies = new java.util.ArrayList<Discrepancy>();

        var matchResult = new MatchQueryResult();
        matchResult.setBlueTotal(1);
        matchResult.setGreenTotal(1);
        matchResult.setDiscrepancies(discrepancies);

        var response = java.util.Collections.singletonMap("match_query_" + query, matchResult);

        when(matchQueryComparisonService.compare(query, size)).thenReturn(response);

        mockMvc.perform(get("/documents/match-query")
                        .param("query", query)
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match_query_" + query + ".blue_total", is(1)))
                .andExpect(jsonPath("$.match_query_" + query + ".green_total", is(1)));
    }

    @Test
    void testCompareMatchQueryWithDefaultParameters() throws Exception {
        String query = "GIRLSDAYSCHOOLTRUST";
        int size = 40;

        var matchResult = new MatchQueryResult();
        matchResult.setBlueTotal(1);
        matchResult.setGreenTotal(1);
        matchResult.setDiscrepancies(new java.util.ArrayList<>());

        var response = java.util.Collections.singletonMap("match_query_" + query, matchResult);

        when(matchQueryComparisonService.compare(query, size)).thenReturn(response);

        mockMvc.perform(get("/documents/match-query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match_query_" + query + ".blue_total", is(1)))
                .andExpect(jsonPath("$.match_query_" + query + ".green_total", is(1)));
    }

    @Test
    void testCompareMatchQueryReturnsInternalServerErrorOnException() throws Exception {
        String query = "GIRLSDAYSCHOOLTRUST";
        int size = 40;

        when(matchQueryComparisonService.compare(query, size))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/documents/match-query")
                        .param("query", query)
                        .param("size", String.valueOf(size)))
                .andExpect(status().isInternalServerError());
    }


}

