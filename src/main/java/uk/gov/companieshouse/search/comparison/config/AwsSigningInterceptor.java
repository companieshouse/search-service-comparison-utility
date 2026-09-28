package uk.gov.companieshouse.search.comparison.config;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Signs outgoing HTTP requests with AWS SigV4 so that calls to an IAM-access-controlled
 * AWS OpenSearch domain are authenticated using the Lambda's execution role,
 * instead of being sent as anonymous requests.
 */
public class AwsSigningInterceptor implements ClientHttpRequestInterceptor {

    private static final String SERVICE_NAME = "es";

    private final AWSCredentialsProvider credentialsProvider;
    private final DefaultAwsRegionProviderChain regionProviderChain = new DefaultAwsRegionProviderChain();

    public AwsSigningInterceptor(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        URI uri = request.getURI();

        DefaultRequest<Void> signableRequest = new DefaultRequest<>(SERVICE_NAME);
        signableRequest.setHttpMethod(HttpMethodName.fromValue(request.getMethod().name()));
        signableRequest.setEndpoint(URI.create(uri.getScheme() + "://" + uri.getAuthority()));
        signableRequest.setResourcePath(uri.getRawPath());
        signableRequest.setContent(new ByteArrayInputStream(body));

        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName(SERVICE_NAME);
        signer.setRegionName(regionProviderChain.getRegion());
        signer.sign(signableRequest, credentialsProvider.getCredentials());

        for (Map.Entry<String, String> header : signableRequest.getHeaders().entrySet()) {
            request.getHeaders().set(header.getKey(), header.getValue());
        }

        return execution.execute(request, body);
    }
}
