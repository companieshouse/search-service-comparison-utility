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

@WebMvcTest(SearchComparisonController.class)
class SearchComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentCountService documentCountService;

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
}

