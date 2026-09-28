
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
import uk.gov.companieshouse.search.comparison.service.MatchQueryComparisonService;

public class SearchComparisonHandler implements RequestHandler<Event, Map<String, Object>> {

    private static final Logger LOG = LoggerFactory.getLogger("search-service-comparison-utility");

    private static final DocumentCountService documentCountService;
    private static final ObjectMapper objectMapper;
    private static MatchQueryComparisonService matchQueryComparisonService;

    static {
        try (var context = new AnnotationConfigApplicationContext(SearchComparisonConfiguration.class)) {
            documentCountService = context.getBean(DocumentCountService.class);
            matchQueryComparisonService = context.getBean(MatchQueryComparisonService.class);
            objectMapper = new ObjectMapper();
        }
    }

    @Override
    public Map<String, Object> handleRequest(Event event, Context context) {
        LOG.info("Starting document comparison");
        LOG.info("Event received: " + event);

        try {
            Map<String, Object> responseMap;
            String operation = event.getDetail().getOperation();
            String query = event.getDetail().getQuery();
            int size = event.getDetail().getSize();

            LOG.info(String.format("Operation: %s, Query: %s, Size: %d", operation, query, size));

            // Route to appropriate service based on event type
            if ("match-query".equals(operation)) {
                LOG.info("Executing match-query comparison");
                var result = matchQueryComparisonService.compare(query, size);
                LOG.info("Successfully compared match query");
                responseMap = objectMapper.convertValue(result, Map.class);
            } else {
                // Default: document counts
                LOG.info("Executing document count retrieval");
                DocumentCountResponse response = documentCountService.getDocumentCounts();
                LOG.info("Successfully retrieved document counts");
                responseMap = objectMapper.convertValue(response, Map.class);
            }
            LOG.info("Returning successful response with status 200");
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



