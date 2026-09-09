package uk.gov.companieshouse.search.comparison.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentCountResponse(
    @JsonProperty("total_documents")
    DocumentCounts totalDocuments
) {
    public record DocumentCounts(long blue, long green) {}
}
