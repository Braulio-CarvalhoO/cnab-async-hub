package br.com.cnab.hub.storage;

import br.com.cnab.hub.config.StorageProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Component
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3;
    private final StorageProperties properties;

    public S3ObjectStorage(StorageProperties properties) {
        this.properties = properties;
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .forcePathStyle(true)
                .build();
    }

    @Override
    public void ensureBuckets() {
        createIfMissing(properties.inputBucket());
        createIfMissing(properties.outputBucket());
    }

    @Override
    public void upload(String bucket, String key, byte[] content, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content)
        );
    }

    @Override
    public byte[] download(String bucket, String key) {
        return s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()).asByteArray();
    }

    private void createIfMissing(String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("404")) {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                return;
            }
            throw ex;
        }
    }
}
