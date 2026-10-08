package uk.gov.companieshouse.search.comparison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.companieshouse.search.comparison.client.upload.S3UploadClient;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;
import uk.gov.companieshouse.search.comparison.model.DocumentCountResponse;

@ExtendWith(MockitoExtension.class)
class ReportUploadServiceTest {

    @Mock
    private S3UploadClient s3UploadClient;

    private ReportUploadService reportUploadService;

    @BeforeEach
    void setUp() {
        reportUploadService = new ReportUploadService(s3UploadClient, new ObjectMapper());
    }

    @Test
    void shouldSerialiseReportAndUploadToS3WithKeyPrefix() {
        var report = new DocumentCountResponse(new DocumentCountResponse.DocumentCounts(1234, 5678));

        reportUploadService.upload("document-counts", report);

        verify(s3UploadClient).uploadFile(contains("document-counts/"), anyString());
    }

    @Test
    void shouldUploadSerialisedJsonContent() throws Exception {
        var report = new DocumentCountResponse(new DocumentCountResponse.DocumentCounts(1234, 5678));

        reportUploadService.upload("document-counts", report);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3UploadClient).uploadFile(anyString(), contentCaptor.capture());

        verify(s3UploadClient).uploadFile(anyString(), contentCaptor.capture());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode actual = objectMapper.readTree(contentCaptor.getValue());
        JsonNode expected = objectMapper.readTree("""
        {
          "total_documents": {
            "blue": 1234,
            "green": 5678
          }
        }
        """);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowSearchComparisonExceptionWhenS3UploadFails() {
        var report = new DocumentCountResponse(new DocumentCountResponse.DocumentCounts(1234, 5678));

        doThrow(S3Exception.builder().message("Upload failed").build())
                .when(s3UploadClient).uploadFile(anyString(), anyString());

        SearchComparisonException exception = assertThrows(SearchComparisonException.class,
                () -> reportUploadService.upload("document-counts", report));

        assertTrue(exception.getMessage().contains("S3"));
    }
}
