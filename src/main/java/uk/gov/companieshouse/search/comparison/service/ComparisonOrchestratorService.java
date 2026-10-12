package uk.gov.companieshouse.search.comparison.service;


import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComparisonOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger("search-service-comparison-utility");

    private static final String DOCUMENT_COUNT = "DOCUMENT_COUNT";
    private static final String MATCH_QUERY = "MATCH_QUERY";
    private static final String DEFAULT_OPERATION = DOCUMENT_COUNT;
    private static final String S3_KEY_PREFIX = "comparison-results";

    private final DocumentCountService documentCountService;
    private final MatchQueryComparisonService matchQueryComparisonService;
    private final ReportUploadService reportUploadService;
    private final String defaultQuery;
    private final int defaultSize;

    public ComparisonOrchestratorService(
            DocumentCountService documentCountService,
            MatchQueryComparisonService matchQueryComparisonService,
            ReportUploadService reportUploadService,
            String defaultQuery,
            int defaultSize) {
        this.documentCountService = documentCountService;
        this.matchQueryComparisonService = matchQueryComparisonService;
        this.reportUploadService = reportUploadService;
        this.defaultQuery = defaultQuery;
        this.defaultSize = defaultSize;
    }

    public Map<String, Object> orchestrate(Event event) {
        if (event == null || event.getDetails() == null || event.getDetails().isEmpty()) {
            logger.info("Orchestration skipped: no event details provided");
            return Map.of("error", "No details provided in event");
        }

        logger.info(String.format("Orchestration started for %d operation(s)", event.getDetails().size()));
        Map<String, Object> combinedResults = new LinkedHashMap<>();

        for (Event.Detail detail : event.getDetails()) {
            String operation = getOperation(detail.getOperation());
            String query = detail.getQuery() != null ? detail.getQuery() : defaultQuery;
            int size = detail.getSize() > 0 ? detail.getSize() : defaultSize;

            Map<String, Object> result = executeOperation(operation, query, size);
            combinedResults.put(operation, result);
        }

        logger.info("Orchestration completed successfully");
        // Save combined results to S3
        // reportUploadService.upload(S3_KEY_PREFIX, combinedResults);
        return combinedResults;
    }


    private String getOperation(String operation) {
        return (operation == null || operation.isEmpty()) ? DEFAULT_OPERATION : operation;
    }

    private Map<String, Object> executeOperation(String operation, String query, int size) {
        try {
            return switch (operation) {
                case DOCUMENT_COUNT -> handleDocumentCount();
                case MATCH_QUERY -> handleMatchQuery(query, size);
                default -> {
                    logger.error(String.format("Unknown operation: %s", operation));
                    yield Map.of("error", "Unknown operation: " + operation);
                }
            };
        } catch (Exception e) {
            logger.error(String.format("Error executing operation: %s", operation), e);
            return Map.of("error", "Operation failed: " + e.getMessage());
        }
    }

    private Map<String, Object> handleDocumentCount() {
        logger.info("Document count retrieval started");
        DocumentCountResponse response = documentCountService.getDocumentCounts();
        if (response == null || response.totalDocuments() == null) {
            logger.info("Document count retrieval returned no data");
            return Map.of("error", "No document count response");
        }
        Map<String, Object> countMap = new HashMap<>();
        countMap.put("blue", response.totalDocuments().blue());
        countMap.put("green", response.totalDocuments().green());
        logger.info("Document count retrieval completed");
        return countMap;
    }

    private Map<String, Object> handleMatchQuery(String query, int size) {
        logger.info(String.format("Match query comparison started: query=[%s], size=[%d]", query, size));

        try {
            Map<String, MatchQueryResult> queryResults = matchQueryComparisonService.compare(query, size);
            if (queryResults == null) {
                logger.info("Match query comparison returned no data");
                return Map.of("error", "No match query results");
            }

            logger.info(String.format("Match query comparison completed: query=[%s], size=[%d]", query, size));
            Map<String, Object> response = new HashMap<>();
            response.putAll(queryResults);
            return response;
        } catch (SearchComparisonException e) {
            logger.error(String.format("Match query comparison failed: %s", e.getMessage()), e);
            return Map.of("error", "Match query comparison failed");
        }
    }


}
