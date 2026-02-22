package soon.fridgely.global.support.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 이미지 파일 검증을 담당
 * - 파일 크기 검증
 * - Content-Type 검증
 * - 파일 확장자 검증
 * - Magic Number 검증 (파일 위변조 방지)
 */
@Slf4j
@Component
public class ImageValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Map<String, byte[]> MAGIC_NUMBERS = Map.of(
        "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
        "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38},
        "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    public void validate(MultipartFile file) {
        validateFileSize(file);
        validateContentType(file);
        validateFileExtension(file);
        validateMagicNumber(file);
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CoreException(ErrorType.FILE_SIZE_EXCEEDED);
        }
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CoreException(ErrorType.INVALID_FILE_TYPE);
        }
    }

    private void validateFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new CoreException(ErrorType.INVALID_FILE_TYPE);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CoreException(ErrorType.INVALID_FILE_TYPE);
        }
    }

    private void validateMagicNumber(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return;
        }

        byte[] expectedMagicNumber = MAGIC_NUMBERS.get(contentType);
        if (expectedMagicNumber == null) {
            return;
        }

        try (InputStream is = file.getInputStream()) {
            byte[] fileHeader = is.readNBytes(expectedMagicNumber.length);

            if (!Arrays.equals(fileHeader, expectedMagicNumber)) {
                log.debug("[ImageValidator] Magic Number 불일치 (ContentType={})", contentType);
                throw new CoreException(ErrorType.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            log.error("[ImageValidator] Magic Number 검증 중 오류", e);
            throw new CoreException(ErrorType.STORAGE_UPLOAD_FAILED);
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

}