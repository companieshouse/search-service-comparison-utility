package uk.gov.companieshouse.search.comparison.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

/**
 * Signs outgoing HTTP requests with AWS SigV4 so that calls to an IAM-access-controlled
 * AWS OpenSearch domain are authenticated using the Lambda's execution role,
 * instead of being sent as anonymous requests.
 */
public class AwsSigningInterceptor implements ClientHttpRequestInterceptor {

    private static final String SERVICE_NAME = "es";

    private final AwsCredentialsProvider credentialsProvider;
    private final DefaultAwsRegionProviderChain regionProviderChain = DefaultAwsRegionProviderChain.builder().build();

    public AwsSigningInterceptor(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        SdkHttpRequest unsignedRequest = SdkHttpRequest.builder()
                .method(SdkHttpMethod.fromValue(request.getMethod().name()))
                .uri(request.getURI())
                .build();

        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        SignRequest<AwsCredentials> signRequest = SignRequest.builder(credentials)
                .request(unsignedRequest)
                .payload(ContentStreamProvider.fromByteArray(body))
                .putProperty(AwsV4HttpSigner.REGION_NAME, regionProviderChain.getRegion().id())
                .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, SERVICE_NAME)
                .build();

        SignedRequest signedRequest = AwsV4HttpSigner.create().sign(signRequest);

        for (Map.Entry<String, List<String>> header : signedRequest.request().headers().entrySet()) {
            request.getHeaders().put(header.getKey(), header.getValue());
        }

        return execution.execute(request, body);
    }
}
