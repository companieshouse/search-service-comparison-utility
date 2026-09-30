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

import java.util.Map;

@RestController
@RequestMapping("/documents")
public class SearchComparisonController {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");
    private final DocumentCountService documentCountService;
    private final MatchQueryComparisonService matchQueryComparisonService;

    public SearchComparisonController(DocumentCountService documentCountService,
                                      MatchQueryComparisonService matchQueryComparisonService) {
        this.documentCountService = documentCountService;
        this.matchQueryComparisonService = matchQueryComparisonService;

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
}
