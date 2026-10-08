package uk.gov.companieshouse.search.comparison.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import uk.gov.companieshouse.search.comparison.client.upload.S3UploadClient;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;
import uk.gov.companieshouse.search.comparison.service.MatchQueryComparisonService;
import uk.gov.companieshouse.search.comparison.service.ReportUploadService;

@Configuration
@PropertySource("classpath:application.properties")
public class SearchComparisonConfiguration {

    // Environment variable used to switch between a local unsigned OpenSearch client and an
    // AWS SigV4-signed client.
    private static final String USE_AWS_SIGV4 = "USE_AWS_SIGV4";

    private final SearchComparisonProperties searchComparisonProperties;

    @Value("${s3.bucket.name}")
    private String s3BucketName;

    public SearchComparisonConfiguration(SearchComparisonProperties searchComparisonProperties) {
        this.searchComparisonProperties = searchComparisonProperties;
    }

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

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    private static boolean useAwsSigV4() {
        return Boolean.parseBoolean(System.getenv(USE_AWS_SIGV4));
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.create();
    }

    @Bean
    public S3UploadClient s3UploadClient(S3Client s3Client) {
        return new S3UploadClient(s3Client, s3BucketName);
    }

    @Bean
    public ReportUploadService s3ReportUploadService(S3UploadClient s3UploadClient, ObjectMapper objectMapper) {
        return new ReportUploadService(s3UploadClient, objectMapper);
    }

    @Bean
    public DocumentCountService documentCount(RestTemplate restTemplate, ReportUploadService reportUploadService) {
        return new DocumentCountService(searchComparisonProperties, restTemplate, reportUploadService);
    }

    @Bean
    public MatchQueryComparisonService matchQueryComparison(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new MatchQueryComparisonService(searchComparisonProperties, restTemplate, objectMapper);
    }

}
