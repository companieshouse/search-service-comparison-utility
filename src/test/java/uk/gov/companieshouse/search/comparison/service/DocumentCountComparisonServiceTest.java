package uk.gov.companieshouse.search.comparison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;

@ExtendWith(MockitoExtension.class)
class DocumentCountComparisonServiceTest {

    @Mock
    private DocumentCountService documentCountService;

    private DocumentCountComparisonService service;

    @BeforeEach
    void setUp() {
        service = new DocumentCountComparisonService(documentCountService);
    }

    @Test
    void getOperationReturnsDocumentCount() {
        assertEquals("DOCUMENT_COUNT", service.getOperation());
    }

    @Test
    void compareReturnsBlueAndGreenCounts() {
        Event.Detail detail = mock(Event.Detail.class);

        when(documentCountService.getDocumentCounts(detail))
                .thenReturn(new DocumentCountResponse(
                        new DocumentCountResponse.DocumentCounts(1234L, 5678L)
                ));

        Map<String, Object> result = service.compare(detail);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1234, ((Number) result.get("blue")).intValue());
        assertEquals(5678, ((Number) result.get("green")).intValue());
        verify(documentCountService).getDocumentCounts(detail);
    }
}