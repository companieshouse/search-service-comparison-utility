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

    // Environment variable used to switch between a local unsigned OpenSearch client and an
    // AWS SigV4-signed client.
    private static final String USE_AWS_SIGV4 = "USE_AWS_SIGV4";

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        if (useAwsSigV4()) {
            restTemplate.getInterceptors()
                .add(new AwsSigningInterceptor(DefaultCredentialsProvider.builder().build()));
        }
        return restTemplate;
    }

    private static boolean useAwsSigV4() {
        return Boolean.parseBoolean(System.getenv(USE_AWS_SIGV4));
    }

    @Bean
    public DocumentCountService documentCount(RestTemplate restTemplate) {
        return new DocumentCountService(blueSearchClusterUrl, greenSearchClusterUrl, indexName, restTemplate);
    }
}


