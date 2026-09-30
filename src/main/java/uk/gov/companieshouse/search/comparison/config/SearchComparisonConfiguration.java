package uk.gov.companieshouse.search.comparison.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;


@Configuration
@PropertySource("classpath:application.properties")
public class SearchComparisonConfiguration {

    @Value("${BLUE_SEARCH_CLUSTER_URL}")
    private String blueSearchClusterUrl;

    @Value("${GREEN_SEARCH_CLUSTER_URL}")
    private String greenSearchClusterUrl;

    @Value("${search.index-name}")
    private String indexName;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors()
            .add(new AwsSigningInterceptor(DefaultCredentialsProvider.builder().build()));
        return restTemplate;
    }

    @Bean
    public DocumentCountService documentCount(RestTemplate restTemplate) {
        return new DocumentCountService(blueSearchClusterUrl, greenSearchClusterUrl, indexName, restTemplate);
    }

}