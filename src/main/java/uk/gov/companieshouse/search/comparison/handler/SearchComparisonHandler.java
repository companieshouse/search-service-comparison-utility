
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
    private static final String STATUS_CODE = "statusCode";
    private static final String ERROR = "error";
    private static final String BODY = "body";

    private static final AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(SearchComparisonConfiguration.class);

    private static final DocumentCountService documentCountService = context.getBean(DocumentCountService.class);
    private static final ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
    private static final MatchQueryComparisonService matchQueryComparisonService =
            context.getBean(MatchQueryComparisonService.class);

    @Override
    public Map<String, Object> handleRequest(Event event, Context context) {
        LOG.info("Starting document comparison");
        LOG.info("Event received: " + event);

        try {

            if (event.getDetail() == null) {
                LOG.error("Event detail is missing");
                return Map.of(STATUS_CODE, 400, ERROR, "Event detail is required");
            }

            Map<String, Object> responseMap;
            String operation = event.getDetail().getOperation();

            // Route to appropriate service based on event type
            if ("match-query".equals(operation)) {
                String query = event.getDetail().getQuery();
                int size = event.getDetail().getSize();

                if (query == null || query.isEmpty() || size <= 0) {
                    LOG.error("Query and size are required for match-query operation");
                    return Map.of(STATUS_CODE, 400, ERROR, "Query and size are required");
                }

                LOG.info(String.format("Operation: %s, Query: %s, Size: %d", operation, query, size));
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
                    STATUS_CODE, 200,
                    BODY, responseMap
            );
        } catch (Exception e) {
            LOG.error("Error retrieving document counts: " + e.getMessage(), e);
            return Map.of(
                    STATUS_CODE, 500,
                    ERROR, e.getMessage()
            );
        }
    }
}



