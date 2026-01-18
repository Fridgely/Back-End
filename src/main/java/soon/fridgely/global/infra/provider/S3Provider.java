package soon.fridgely.global.infra.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import soon.fridgely.global.infra.properties.S3Properties;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Profile("live")
public class S3Provider implements StorageProvider {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3Provider(S3Client s3Client, S3Presigner s3Presigner, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = s3Properties.s3().bucket();
    }

    @Override
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        String finalContentType = (contentType != null && !contentType.isBlank())
            ? contentType
            : "application/octet-stream";

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(finalContentType)
                .build();

            RequestBody requestBody = RequestBody.fromInputStream(inputStream, contentLength);

            s3Client.putObject(request, requestBody);
            return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
                )
                .toExternalForm();
        } catch (S3Exception | SdkClientException e) {
            log.error("[S3Provider] S3 파일 업로드 실패. (Key={})", key, e);
            throw new CoreException(ErrorType.STORAGE_UPLOAD_FAILED, "key: " + key);
        }
    }

    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            s3Client.deleteObject(request);
        } catch (S3Exception | SdkClientException e) {
            log.error("[S3Provider] S3 파일 삭제 실패. (Key={})", key, e);
            throw new CoreException(ErrorType.STORAGE_DELETE_FAILED, "key: " + key);
        }
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build()
            );
            return presignedRequest.url().toString();
        } catch (S3Exception | SdkClientException e) {
            log.error("[S3Provider] S3 Presigned URL (GET) 생성 실패. (Key={})", key, e);
            throw new CoreException(ErrorType.STORAGE_PRESIGNED_URL_FAILED, "key: " + key);
        }
    }

}