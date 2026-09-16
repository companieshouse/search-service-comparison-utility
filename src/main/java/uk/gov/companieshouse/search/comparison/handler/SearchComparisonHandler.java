
package uk.gov.companieshouse.search.comparison.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.config.SearchComparisonConfiguration;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;

public class SearchComparisonHandler implements RequestHandler<Event, Map<String, Object>> {

    private static final Logger LOG = LoggerFactory.getLogger("search-service-comparison-utility");

    private static final DocumentCountService documentCountService;
    private static final ObjectMapper objectMapper;

    static {
        try (var context = new AnnotationConfigApplicationContext(SearchComparisonConfiguration.class)) {
            documentCountService = context.getBean(DocumentCountService.class);
            objectMapper = new ObjectMapper();
        }
    }

    @Override
    public Map<String, Object> handleRequest(Event event, Context context) {
        LOG.info("Starting document comparison");
        LOG.info("Event received: " + event);

        try {
            DocumentCountResponse response = documentCountService.getDocumentCounts();
            LOG.info("Successfully retrieved document counts");

            Map<String, Object> responseMap = objectMapper.convertValue(response, Map.class);
            return Map.of(
                    "statusCode", 200,
                    "body", responseMap
            );
        } catch (Exception e) {
            LOG.error("Error retrieving document counts: " + e.getMessage(), e);
            return Map.of(
                    "statusCode", 500,
                    "error", e.getMessage()
            );
        }
    }
}
