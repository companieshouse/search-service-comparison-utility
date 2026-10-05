package uk.gov.companieshouse.search.comparison.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.model.MatchQueryResult;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import uk.gov.companieshouse.search.comparison.service.MatchQueryComparisonService;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.service.ComparisonOrchestratorService;


import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/documents")
public class SearchComparisonController {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");
    private final DocumentCountService documentCountService;
    private final MatchQueryComparisonService matchQueryComparisonService;
    private final ComparisonOrchestratorService comparisonOrchestratorService;


    public SearchComparisonController(DocumentCountService documentCountService,
                                      MatchQueryComparisonService matchQueryComparisonService,
                                      ComparisonOrchestratorService comparisonOrchestratorService) {
        this.documentCountService = documentCountService;
        this.matchQueryComparisonService = matchQueryComparisonService;
        this.comparisonOrchestratorService = comparisonOrchestratorService;

    }

    @GetMapping("/count")
    public ResponseEntity<DocumentCountResponse> getDocumentCount() {
        LOGGER.info("Get Document count");
        try {
            DocumentCountResponse response = documentCountService.getDocumentCounts();
            LOGGER.info("Successfully retrieved document counts");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("Error retrieving document counts: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/match-query")
    public ResponseEntity<Map<String, MatchQueryResult>> compareMatchQuery(
            @RequestParam(value = "query", defaultValue = "GIRLONTHEGROUND") String query,
            @RequestParam(value = "size", defaultValue = "40") int size) {
        LOGGER.info("Compare match query for ordered alpha key: " + query);
        try {
            Map<String, MatchQueryResult> result = matchQueryComparisonService.compare(query, size);
            LOGGER.info("Match query comparison completed for: " + query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("Error during match query comparison: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareDocuments(@RequestBody Event event) {
        LOGGER.info("Starting document comparison for operations: " + event.getDetails().stream()
                .map(detail -> detail.getOperation())
                .collect(Collectors.toList()));
        try {
            Map<String, Object> result = comparisonOrchestratorService.orchestrate(event);
            LOGGER.info("Document comparison completed successfully");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("Error during document comparison: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
