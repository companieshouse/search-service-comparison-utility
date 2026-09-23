package uk.gov.companieshouse.search.comparison.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CountResponse(
    @JsonProperty("count")
    long count
) {
}
