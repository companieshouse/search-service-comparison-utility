package uk.gov.companieshouse.search.comparison.client.upload;

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3UploadClient {

    private final S3Client s3Client;
    private final String bucketName;

    public S3UploadClient(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void uploadFile(String key, String content) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("application/json")
                        .build(),
                RequestBody.fromString(content, StandardCharsets.UTF_8));
    }
}
