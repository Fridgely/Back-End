package soon.fridgely.global.infra.provider;

import java.io.InputStream;
import java.time.Duration;

public interface StorageProvider {

    /*
     * 파일을 업로드 하고 접근 가능한 URL을 반환하는 메서드
     */
    String upload(String key, InputStream inputStream, long contentLength, String contentType);

    /*
     * 저장된 파일을 삭제하는 메서드
     */
    void delete(String key);

    /*
     * 임시로 접근 가능한 URL을 생성하는 메서드
     */
    String generatePresignedUrl(String key, Duration expiration);

}