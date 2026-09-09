package uk.gov.companieshouse.search.comparison.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchResponse(
    @JsonProperty("hits")
    Hits hits
) {

    public record Hits(
        @JsonProperty("total")
        Total total
    ) {}

    public record Total(
        @JsonProperty("value")
        long value,

        @JsonProperty("relation")
        String relation
    ) {}
}
