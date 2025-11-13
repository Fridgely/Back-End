package soon.fridgely.global.support.provider;

import java.time.Duration;

public interface StorageProvider {

    /*
     * 파일을 업로드 하고 접근 가능한 URL을 반환하는 메서드
     */
    String upload(String key, byte[] data, String contentType);

    /*
     * 저장된 파일을 다운로드 하는 메서드
     */
    byte[] download(String key);

    /*
     * 저장된 파일을 삭제하는 메서드
     */
    void delete(String key);

    /*
     * 임시로 접근 가능한 URL을 생성하는 메서드
     */
    String generatePresignedUrl(String key, Duration expiration);

}