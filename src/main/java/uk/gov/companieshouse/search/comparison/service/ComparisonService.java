package uk.gov.companieshouse.search.comparison.service;

import java.util.Map;
import uk.gov.companieshouse.search.comparison.handler.Event;

public interface ComparisonService {

    /**
     * Returns the operation type this service handles.
     * Must match the operation field in Event.Detail.
     *
     * @return operation identifier (e.g., "DOCUMENT_COUNT", "MATCH_QUERY")
     */
    String getOperation();

    /**
     * Executes comparison for the given detail.
     *
     * @param detail the comparison operation details
     * @return comparison results as a map
     */
    Map<String, Object> compare(Event.Detail detail);
}
