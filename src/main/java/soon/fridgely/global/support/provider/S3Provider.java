package soon.fridgely.global.support.provider;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.properties.S3Properties;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
public class S3Provider implements StorageProvider {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner; // TODO: 차후 presigned URL 기능 구현 시 사용
    private final String bucketName;

    public S3Provider(S3Client s3Client, S3Presigner s3Presigner, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = s3Properties.s3().bucket();
    }

    @Override
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            contentType = inferContentType(key);
        }
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
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
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: key = {}", key, e);
            throw new CoreException(ErrorType.STORAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String key) {
        throw new UnsupportedOperationException("Unsupported delete");
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiration) {
        throw new UnsupportedOperationException("Unsupported generatePresignedUrl");
    }

    private String inferContentType(String key) {
        int lastIndex = key.lastIndexOf('.');
        if (lastIndex == -1 || lastIndex == key.length() - 1) {
            return "application/octet-stream";
        }

        String extension = key.substring(lastIndex + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
    }

}