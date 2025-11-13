package soon.fridgely.global.support.provider;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import soon.fridgely.global.support.properties.S3Properties;

import java.time.Duration;

@Slf4j
public class S3Provider implements StorageProvider {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3Provider(S3Properties s3Properties) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
            s3Properties.credentials().accessKey(),
            s3Properties.credentials().secretKey()
        );

        Region region = Region.of(s3Properties.region().staticRegion());

        this.s3Client = S3Client.builder()
            .region(region)
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build();

        this.s3Presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build();

        this.bucketName = s3Properties.s3().bucket();
    }

    @Override
    public String upload(String key, byte[] data, String contentType) {
        throw new UnsupportedOperationException("Unsupported upload");
    }

    @Override
    public byte[] download(String key) {
        throw new UnsupportedOperationException("Unsupported download");
    }

    @Override
    public void delete(String key) {
        throw new UnsupportedOperationException("Unsupported delete");
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiration) {
        throw new UnsupportedOperationException("Unsupported generatePresignedUrl");
    }
}