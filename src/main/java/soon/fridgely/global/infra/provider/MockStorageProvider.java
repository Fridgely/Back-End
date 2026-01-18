package soon.fridgely.global.infra.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Profile({"local", "test", "performance"})
@Component
public class MockStorageProvider implements StorageProvider {

    private static final String MOCK_URL_PREFIX = "https://mock-storage.example.com/";

    @Override
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        log.info("[MockStorage] Upload - Key: {}, ContentLength: {}, ContentType: {}", key, contentLength, contentType);
        return MOCK_URL_PREFIX + key;
    }

    @Override
    public void delete(String key) {
        log.info("[MockStorage] Delete - Key: {}", key);
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiration) {
        log.info("[MockStorage] GeneratePresignedUrl - Key: {}, Expiration: {}", key, expiration);
        return MOCK_URL_PREFIX + key + "?token=" + UUID.randomUUID();
    }

}