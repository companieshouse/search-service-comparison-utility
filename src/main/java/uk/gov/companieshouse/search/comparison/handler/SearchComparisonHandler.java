
package uk.gov.companieshouse.search.comparison.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.config.SearchComparisonConfiguration;
import uk.gov.companieshouse.search.comparison.service.ComparisonOrchestratorService;

public class SearchComparisonHandler implements RequestHandler<Event, Map<String, Object>> {

    private static final Logger LOG = LoggerFactory.getLogger("search-service-comparison-utility");
    private static final String STATUS_CODE = "statusCode";
    private static final String ERROR = "error";
    private static final String BODY = "body";

    private static final ObjectMapper objectMapper;
    private static final ComparisonOrchestratorService orchestratorService;

    static {
        try (var context = new AnnotationConfigApplicationContext(SearchComparisonConfiguration.class)) {
            orchestratorService = context.getBean(ComparisonOrchestratorService.class);
            objectMapper = context.getBean(ObjectMapper.class);
        }
    }

    @Override
    public Map<String, Object> handleRequest(Event event, Context context) {
        LOG.info("Starting document comparison");
        LOG.info("Event received: " + event);

        try {

            if (event.getDetails() == null) {
                LOG.error("Event detail is missing");
                return Map.of(STATUS_CODE, 400, ERROR, "Event detail is required");
            }

            Map<String, Object> responseMap;
            LOG.info("Executing orchestration service for event");
            var result = orchestratorService.orchestrate(event);
            LOG.info("Successfully executed orchestration service");
            responseMap = objectMapper.convertValue(result, Map.class);
            LOG.info("Returning successful response with status 200");
            return Map.of(
                    STATUS_CODE, 200,
                    BODY, responseMap
            );
        } catch (Exception e) {
            LOG.error("Failed to execute orchestration service: " + e.getMessage(), e);
            return Map.of(
                    STATUS_CODE, 500,
                    ERROR, e.getMessage()
            );
        }
    }
}



