package uk.gov.companieshouse.search.comparison.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.search.comparison.SearchComparisonServiceApplication;

public class SearchDocumentComparisonHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("search-service-comparison-utility");

    private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(SearchComparisonServiceApplication.class);
        } catch (ContainerInitializationException e) {
            LOGGER.error("Error could not initialize application", e);
            throw new RuntimeException("Could not initialize application", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
