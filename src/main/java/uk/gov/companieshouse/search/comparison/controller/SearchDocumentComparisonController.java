package uk.gov.companieshouse.search.comparison.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;
import java.util.Map;

@RestController
@RequestMapping("/documents")
public class SearchDocumentComparisonController {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");
    private final DocumentCountService documentCountService;

    public SearchDocumentComparisonController(DocumentCountService documentCountService) {
        this.documentCountService = documentCountService;
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

    @PostMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareSearchIndexes() {
        try {
            LOGGER.info("Comparison - Blue and Green search results");
            Map<String, Object> result = Map.of();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("Error comparing indexes: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
