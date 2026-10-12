package uk.gov.companieshouse.search.comparison.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
public record SearchComparisonProperties(
        @Value("${BLUE_SEARCH_CLUSTER_URL}") String blueSearchClusterUrl,
        @Value("${GREEN_SEARCH_CLUSTER_URL}") String greenSearchClusterUrl,
        @Value("${search.index-name}") String indexName) {

}
