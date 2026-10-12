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
import uk.gov.companieshouse.search.comparison.service.ComparisonOrchestratorService;
import uk.gov.companieshouse.search.comparison.service.DocumentCountComparisonService;
import uk.gov.companieshouse.search.comparison.service.DocumentCountService;
import uk.gov.companieshouse.search.comparison.service.MatchQueryComparisonService;
import uk.gov.companieshouse.search.comparison.service.ComparisonService;
import uk.gov.companieshouse.search.comparison.service.ReportUploadService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;

@Configuration
@PropertySource("classpath:application.properties")
public class SearchComparisonConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger("search-service-comparison-utility");

    @Value("${search.default-query}")
    private String defaultQuery;

    @Value("${search.default-size}")
    private int defaultSize;

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
        //return Boolean.parseBoolean(System.getenv(USE_AWS_SIGV4));
        return false; // For local testing, set to false. In production, set to true.
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

    /**
     * Strategy: DocumentCountComparisonService
     * Wraps DocumentCountService to implement ComparisonService interface.
     */
    @Bean
    public DocumentCountComparisonService documentCountComparisonService(
            DocumentCountService documentCountService) {
        LOG.info("Creating DocumentCountComparisonService strategy bean");
        return new DocumentCountComparisonService(documentCountService);
    }

    /**
     * Strategy: MatchQueryComparisonService
     * Implement ComparisonService interface directly.
     */
    @Bean
    public MatchQueryComparisonService matchQueryComparison(RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new MatchQueryComparisonService(searchComparisonProperties, restTemplate, objectMapper);
    }

    /**
     * Aggregates all ComparisonService implementations into a list.
     * Spring automatically collects all beans implementing ComparisonService.
     * New strategies are automatically registered when added as @Bean.
     */
    @Bean
    public List<ComparisonService> comparisonServices(
            DocumentCountComparisonService documentCountService,
            MatchQueryComparisonService matchQueryService) {
        LOG.info("Registering comparison service strategies");
        return List.of(
                documentCountService,
                matchQueryService
                // New strategies added here
        );
    }

    @Bean
    public ComparisonOrchestratorService comparisonOrchestratorService(
            List<ComparisonService> comparisonServices,
            ReportUploadService reportUploadService) {
        return new ComparisonOrchestratorService(
                comparisonServices,
                reportUploadService,
                defaultQuery,
                defaultSize
        );
    }


}
