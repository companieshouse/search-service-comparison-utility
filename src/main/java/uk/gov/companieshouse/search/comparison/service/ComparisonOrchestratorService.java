package uk.gov.companieshouse.search.comparison.service;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.handler.Event;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComparisonOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger("search-service-comparison-utility");
    private static final String S3_KEY_PREFIX = "comparison-report";
    private static final String ERROR = "error";

    private final Map<String, ComparisonService> services;
    private final ReportUploadService reportUploadService;

    /**
     * Constructor that accepts a list of ComparisonService implementations.
     * Builds an internal map for O(1) lookup by operation type.
     *
     * @param serviceList list of all available comparison services
     * @param reportUploadService  service for uploading reports to S3
     */
    public ComparisonOrchestratorService(
            List<ComparisonService> serviceList,
            ReportUploadService reportUploadService) {
        this.services = serviceList.stream()
                .collect(Collectors.toMap(
                        ComparisonService::getOperation,
                        service -> service
                ));
        this.reportUploadService = reportUploadService;

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
            logger.info("No details provided in event");
            return Map.of(ERROR, "No details provided in event");
        }

        logger.info(String.format("Orchestration started for %d operation(s)", event.getDetails().size()));
        Map<String, Object> combinedResults = new LinkedHashMap<>();

        for (Event.Detail detail : event.getDetails()) {
            String operation = detail.getOperation();

            logger.info("Processing operation: " + operation);
            ComparisonService service = services.get(operation);

            if (service == null) {
                logger.error("Unknown operation: " + operation);
                combinedResults.put(
                        operation,
                        Map.of(ERROR, "Unknown operation: " + operation)
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
                        Map.of(ERROR, "Failed to execute " + operation + ": " + e.getMessage())
                );
            }
        }

        logger.info("Orchestration completed with " + combinedResults.size() + " operation results");
        // Save combined results to S3
           reportUploadService.upload(S3_KEY_PREFIX, combinedResults);
        return combinedResults;
    }
}
