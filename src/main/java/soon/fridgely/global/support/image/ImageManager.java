package soon.fridgely.global.support.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.global.infra.provider.StorageProvider;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 이미지 업로드/삭제를 담당
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ImageManager {
    private static final String IMAGE_KEY_PREFIX = "images/";

    private final StorageProvider storageProvider;
    private final ImageValidator imageValidator;

    /**
     * 이미지 업로드
     *
     * @return 업로드된 이미지 URL (실패 시 예외 발생)
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        imageValidator.validate(file);
        String key = generateKey(file.getOriginalFilename());

        try (InputStream is = file.getInputStream()) {
            return storageProvider.upload(key, is, file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("[ImageManager] 파일 업로드 중 IOException 발생. (Key={})", key, e);
            throw new CoreException(ErrorType.STORAGE_UPLOAD_FAILED, "key: " + key);
        }
    }

    /**
     * 이미지 삭제
     *
     * @param imageUrl 삭제할 이미지 URL
     */
    public void delete(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }

        String key = extractKeyFromUrl(imageUrl);
        if (key == null) {
            throw new CoreException(ErrorType.INVALID_IMAGE_URL, "url: " + imageUrl);
        }

        try {
            storageProvider.delete(key);
            log.info("[ImageManager] 이미지 삭제 완료. Key: {}", key);
        } catch (Exception e) {
            log.error("[ImageManager] 이미지 삭제 실패. Key: {}", key, e);
            throw new CoreException(ErrorType.STORAGE_DELETE_FAILED, "key: " + key);
        }
    }

    private String generateKey(String originalName) {
        String fileName = (originalName == null || originalName.isBlank())
            ? "unknown"
            : originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return IMAGE_KEY_PREFIX + UUID.randomUUID() + "-" + fileName;
    }

    /**
     * URL에서 S3 키를 추출
     * (imageUrl의 null/empty 검증은 호출자에서 완료됨)
     * 쿼리 파라미터(?)와 프래그먼트(#)는 제거하고 순수 키만 반환
     */
    private String extractKeyFromUrl(String imageUrl) {
        int imagesIndex = imageUrl.indexOf(IMAGE_KEY_PREFIX);
        if (imagesIndex == -1) {
            log.warn("[ImageManager] URL에서 키 추출 실패. URL: {}", imageUrl);
            return null;
        }

        // ? 또는 # 위치 찾기
        int queryIndex = imageUrl.indexOf('?', imagesIndex);
        int fragmentIndex = imageUrl.indexOf('#', imagesIndex);

        // 둘 중 먼저 나오는 위치를 endIndex로 사용 (없으면 -1)
        int endIndex = -1;
        if (queryIndex != -1 && fragmentIndex != -1) {
            endIndex = Math.min(queryIndex, fragmentIndex);
        } else if (queryIndex != -1) {
            endIndex = queryIndex;
        } else if (fragmentIndex != -1) {
            endIndex = fragmentIndex;
        }

        return endIndex != -1
            ? imageUrl.substring(imagesIndex, endIndex)
            : imageUrl.substring(imagesIndex);
    }

}