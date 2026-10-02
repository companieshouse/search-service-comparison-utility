package uk.gov.companieshouse.search.comparison.client.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3UploadClientTest {

    private static final String BUCKET_NAME = "test-bucket";

    @Mock
    private S3Client s3Client;

    private S3UploadClient s3UploadClient;

    @BeforeEach
    void setUp() {
        s3UploadClient = new S3UploadClient(s3Client, BUCKET_NAME);
    }

    @Test
    void testUploadFilePutsObjectToConfiguredBucketAndKey() {
        String key = "document-counts/test.json";
        String content = "{\"total_documents\":{\"blue\":1,\"green\":2}}";

        s3UploadClient.uploadFile(key, content);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, request.bucket());
        assertEquals(key, request.key());
        assertEquals("application/json", request.contentType());
    }
}
