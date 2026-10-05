package uk.gov.companieshouse.search.comparison.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComparisonOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(ComparisonOrchestratorService.class);
    private static final String DOCUMENT_COUNT = "DOCUMENT_COUNT";
    private static final String MATCH_QUERY = "MATCH_QUERY";
    private static final String DEFAULT_OPERATION = DOCUMENT_COUNT;

    private final DocumentCountService documentCountService;
    private final MatchQueryComparisonService matchQueryComparisonService;
    private final String defaultQuery;
    private final int defaultSize;

    public ComparisonOrchestratorService(
            DocumentCountService documentCountService,
            MatchQueryComparisonService matchQueryComparisonService,
            String defaultQuery,
            int defaultSize) {
        this.documentCountService = documentCountService;
        this.matchQueryComparisonService = matchQueryComparisonService;
        this.defaultQuery = defaultQuery;
        this.defaultSize = defaultSize;
    }

    public Map<String, Object> orchestrate(Event event) {
        if (event == null || event.getDetails() == null || event.getDetails().isEmpty()) {
            logger.warn("No details provided in event");
            return Map.of("error", "No details provided in event");
        }

        Map<String, Object> combinedResults = new LinkedHashMap<>();

        for (Event.Detail detail : event.getDetails()) {
            String operation = getOperation(detail.getOperation());
            String query = detail.getQuery() != null ? detail.getQuery() : defaultQuery;
            int size = detail.getSize() > 0 ? detail.getSize() : defaultSize;

            Map<String, Object> result = executeOperation(operation, query, size);
            combinedResults.put(operation, result);
        }

        // Save combined results to S3
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
                    logger.error("Unknown operation: {}", operation);
                    yield Map.of("error", "Unknown operation: " + operation);
                }
            };
        } catch (Exception e) {
            logger.error("Error executing operation: {}", operation, e);
            return Map.of("error", "Operation failed: " + e.getMessage());
        }
    }

    private Map<String, Object> handleDocumentCount() {
        logger.info("Executing document count retrieval");
        DocumentCountResponse response = documentCountService.getDocumentCounts();
        if (response == null || response.totalDocuments() == null) {
            logger.warn("No document count response received");
            return Map.of("error", "No document count response");
        }
        Map<String, Object> countMap = new HashMap<>();
        countMap.put("blue", response.totalDocuments().blue());
        countMap.put("green", response.totalDocuments().green());
        logger.info("Successfully retrieved document counts");
        return countMap;
    }

    private Map<String, Object> handleMatchQuery(String query, int size) {
        logger.info("Executing match-query comparison with query: {}, size: {}", query, size);
        Map<String, MatchQueryResult> queryResults = matchQueryComparisonService.compare(query, size);
        if (queryResults == null) {
            logger.warn("No match query results received");
            return Map.of("error", "No match query results");
        }
        logger.info("Successfully compared match query");
        return new HashMap<>(queryResults);
    }


}
