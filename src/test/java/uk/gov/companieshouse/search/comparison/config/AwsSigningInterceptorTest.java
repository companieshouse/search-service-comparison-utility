package uk.gov.companieshouse.search.comparison.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import java.net.URI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

@ExtendWith(MockitoExtension.class)
class AwsSigningInterceptorTest {

    @Mock
    private AWSCredentialsProvider credentialsProvider;

    @Mock
    private ClientHttpRequestExecution httpRequestExecution;

    @Mock
    private ClientHttpResponse expectedResponse;

    private AwsSigningInterceptor interceptor;

    @BeforeAll
    static void setUpRegion() {
        System.setProperty("aws.region", "eu-west-2");
    }

    @BeforeEach
    void setUp() {
        interceptor = new AwsSigningInterceptor(credentialsProvider);
    }

    @Test
    void shouldInterceptAndSignsRequest() throws Exception {
        AWSCredentials credentials = new BasicAWSCredentials("access-key", "secret-key");
        when(credentialsProvider.getCredentials()).thenReturn(credentials);

        MockClientHttpRequest request = new MockClientHttpRequest(
            HttpMethod.GET,
            URI.create("https://vpc-test-domain.eu-west-2.es.amazonaws.com/alpha_search/_count"));
        byte[] body = new byte[0];

        when(httpRequestExecution.execute(request, body)).thenReturn(expectedResponse);

        ClientHttpResponse actualResponse = interceptor.intercept(request, body, httpRequestExecution);

        assertNotNull(actualResponse);
        verify(httpRequestExecution).execute(request, body);

        String authorizationHeader = request.getHeaders().getFirst("Authorization");
        assertNotNull(authorizationHeader, "Authorization header should be set by the signer");
        assertTrue(authorizationHeader.startsWith("AWS4-HMAC-SHA256"));
        assertNotNull(request.getHeaders().getFirst("X-Amz-Date"));
    }
}
