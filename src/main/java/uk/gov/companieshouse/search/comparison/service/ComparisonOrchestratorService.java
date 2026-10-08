package uk.gov.companieshouse.search.comparison.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;
import uk.gov.companieshouse.search.comparison.service.ComparisonService;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComparisonOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(ComparisonOrchestratorService.class);

   // private final DocumentCountService documentCountService;
   // private final MatchQueryComparisonService matchQueryComparisonService;
    private final Map<String, ComparisonService> services;
    private final String defaultQuery;
    private final int defaultSize;

    /**
     * Constructor that accepts a list of ComparisonService implementations.
     * Builds an internal map for O(1) lookup by operation type.
     *
     * @param serviceList list of all available comparison services
     * @param defaultQuery default query string for operations
     * @param defaultSize default result size for operations
     */
    public ComparisonOrchestratorService(
            //DocumentCountService documentCountService,
            //MatchQueryComparisonService matchQueryComparisonService,
            List<ComparisonService> serviceList,
            String defaultQuery,
            int defaultSize) {
        //this.documentCountService = documentCountService;
        //this.matchQueryComparisonService = matchQueryComparisonService;
        this.services = serviceList.stream()
                .collect(Collectors.toMap(
                        ComparisonService::getOperation,
                        service -> service
                ));
        this.defaultQuery = defaultQuery;
        this.defaultSize = defaultSize;

        logger.info("ComparisonOrchestratorService initialized with " + serviceList.size() + " comparison strategies");
        logger.info("Registered operations: " + this.services.keySet());
    }

    /**
     * Orchestrates comparison operations based on event details.
     * Routes each detail to the appropriate ComparisonService based on operation type.
     *
     * @param event the comparison event containing details
     * @return combined results from all operations
     */
    public Map<String, Object> orchestrate(Event event) {
        if (event == null || event.getDetails() == null || event.getDetails().isEmpty()) {
            logger.warn("No details provided in event");
            return Map.of("error", "No details provided in event");
        }

        logger.info("Starting orchestration for event with " + event.getDetails().size() + " details");

        Map<String, Object> combinedResults = new LinkedHashMap<>();

        for (Event.Detail detail : event.getDetails()) {
            String operation = detail.getOperation();

            // Use hardcoded default if operation is null or empty
//            if (operation == null || operation.isEmpty()) {
//                operation = "MATCH_QUERY"; // default operation
//            }

            // Use default values if not provided in event
//            String query = detail.getQuery() != null ? detail.getQuery() : defaultQuery;
//            int size = detail.getSize() > 0 ? detail.getSize() : defaultSize;
            logger.info("Processing operation: " + operation);
            ComparisonService service = services.get(operation);

            if (service == null) {
                logger.error("Unknown operation: " + operation);
                combinedResults.put(
                        operation,
                        Map.of("error", "Unknown operation: " + operation)
                );
                continue;
            }

            try {
                Map<String, Object> result = service.compare(detail);
                combinedResults.put(operation, result);
                logger.info("Operation " + operation + " completed successfully");
            } catch (Exception e) {
                logger.error("Operation " + operation + " failed: " + e.getMessage(), e);
                combinedResults.put(
                        operation,
                        Map.of("error", "Failed to execute " + operation + ": " + e.getMessage())
                );
            }
        }

        logger.info("Orchestration completed with " + combinedResults.size() + " operation results");
        // Save combined results to S3
       // s3StorageService.save(combinedResults);
        return combinedResults;
    }
}
