package uk.gov.companieshouse.search.comparison.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;

@Configuration
public class SearchComparisonServiceConfiguration {

    @Value("${BLUE_SEARCH_CLUSTER_URL}")
    private String blueSearchClusterUrl;

    @Value("${GREEN_SEARCH_CLUSTER_URL}")
    private String greenSearchClusterUrl;

    @Value("${search.index-name}")
    private String indexName;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public DocumentCountService documentCount(RestTemplate restTemplate) {
        return new DocumentCountService(blueSearchClusterUrl, greenSearchClusterUrl, indexName, restTemplate);
    }
}

