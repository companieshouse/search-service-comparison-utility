package uk.gov.companieshouse.search.comparison.service;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.handler.Event;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;

public class DocumentCountComparisonService implements ComparisonService {

    private static final Logger LOG = LoggerFactory.getLogger("search-service-comparison-utility");

    private final DocumentCountService documentCountService;

    public DocumentCountComparisonService(DocumentCountService documentCountService) {
        this.documentCountService = documentCountService;
    }

    @Override
    public String getOperation() {
        return "DOCUMENT_COUNT";
    }

    @Override
    public Map<String, Object> compare(Event.Detail detail) {
        LOG.info("Executing DOCUMENT_COUNT comparison strategy");

        DocumentCountResponse response = documentCountService.getDocumentCounts();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("blue", response.totalDocuments().blue());
        result.put("green", response.totalDocuments().green());

        LOG.info("DOCUMENT_COUNT comparison completed successfully");
        return result;
    }
}
