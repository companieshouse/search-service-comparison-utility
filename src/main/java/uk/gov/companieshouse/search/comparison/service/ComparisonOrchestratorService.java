package uk.gov.companieshouse.search.comparison.service;

import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComparisonOrchestratorService {
    private final DocumentCountService documentCountService;
    private final MatchQueryComparisonService matchQueryComparisonService;
    private final String defaultQuery;
    private final int defaultSize;

    //  private final MatchPrefixComparisonService matchPrefixComparisonService;
   // private final S3StorageService s3StorageService;

//    public ComparisonOrchestratorService(
//            DocumentCountService documentCountService,
//            MatchQueryComparisonService matchQueryComparisonService,
//            MatchPrefixComparisonService matchPrefixComparisonService,
//            S3StorageService s3StorageService) {
//        this.documentCountService = documentCountService;
//        this.matchQueryComparisonService = matchQueryComparisonService;
//        this.matchPrefixComparisonService = matchPrefixComparisonService;
//        this.s3StorageService = s3StorageService;
//    }

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
        Map<String, Object> combinedResults = new LinkedHashMap<>();

        for (Event.Detail detail : event.getDetails()) {
            String operation = detail.getOperation();

            // Use hardcoded default if operation is null or empty
            if (operation == null || operation.isEmpty()) {
                operation = "MATCH_QUERY"; // default operation
            }

            // Use default values if not provided in event
            String query = detail.getQuery() != null ? detail.getQuery() : defaultQuery;
            int size = detail.getSize() > 0 ? detail.getSize() : defaultSize;

            Map<String, Object> result = switch (operation) {
                //case "DOCUMENT_COUNT" -> documentCountService.compare();
               // case "DOCUMENT_COUNT" -> (Map<String, Object>) (Object) documentCountService.getDocumentCounts();
                case "DOCUMENT_COUNT" -> {
                    DocumentCountResponse response = documentCountService.getDocumentCounts();
                    Map<String, Object> countMap = new HashMap<>();
                    countMap.put("blue", response.totalDocuments().blue());
                    countMap.put("green", response.totalDocuments().green());
                    yield countMap;
                }
                case "MATCH_QUERY" -> (Map<String, Object>) (Object) matchQueryComparisonService.compare(query, size);

            //    The second approach is cleaner and ensures "green": null displays properly if the count is null (thanks to your ObjectMapper configuration).
//                case "DOCUMENT_COUNT" -> {
//                    DocumentCountResponse response = documentCountService.getDocumentCounts();
//                    yield Map.of(
//                            "blue", response.getBlueCount(),
//                            "green", response.getGreenCount()
//                    );
//                };
                //The second approach is cleaner since MatchQueryResult is a subtype that can be cast to Object implicitly in a HashMap<String, Object>.
//                case "MATCH_QUERY" -> {
//                    Map<String, MatchQueryResult> queryResults = matchQueryComparisonService.compare(detail.getQuery(), detail.getSize());
//                    yield new HashMap<>(queryResults);
//                };



                //case "MATCH_QUERY" -> matchQueryComparisonService.compare(detail.getQuery(), detail.getSize());
              //  case "MATCH_PREFIX" -> matchPrefixComparisonService.compare(detail.getQuery());
                default -> Map.of("error", "Unknown operation: " + operation);
            };

            combinedResults.put(operation, result);
        }

        // Save combined results to S3
       // s3StorageService.save(combinedResults);

        return combinedResults;
    }
}
