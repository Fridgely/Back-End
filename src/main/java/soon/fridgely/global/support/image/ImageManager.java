package soon.fridgely.global.support.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.global.infra.provider.StorageProvider;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class ImageManager {

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

    private final StorageProvider storageProvider;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validateImage(file);
        String key = generateKey(file.getOriginalFilename());

        try (InputStream is = file.getInputStream()) {
            return storageProvider.upload(key, is, file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("[ImageManager] 파일 업로드 중 IOException 발생. (Key={})", key, e);
            throw new CoreException(ErrorType.STORAGE_UPLOAD_FAILED, "key: " + key);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CoreException(ErrorType.FILE_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CoreException(ErrorType.INVALID_FILE_TYPE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new CoreException(ErrorType.INVALID_FILE_TYPE);
            }
        }

        validateMagicNumber(file, contentType);
    }

    private void validateMagicNumber(MultipartFile file, String contentType) {
        byte[] expectedMagicNumber = MAGIC_NUMBERS.get(contentType);
        if (expectedMagicNumber == null) {
            return;
        }

        try (InputStream is = file.getInputStream()) {
            byte[] fileHeader = is.readNBytes(expectedMagicNumber.length);

            if (!Arrays.equals(fileHeader, expectedMagicNumber)) {
                log.warn("[ImageManager] Magic Number 불일치. (ContentType={}, ActualHeader={})",
                    contentType, Arrays.toString(fileHeader));
                throw new CoreException(ErrorType.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            log.error("[ImageManager] Magic Number 검증 중 오류.", e);
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

    private String generateKey(String originalName) {
        String fileName = (originalName == null || originalName.isBlank())
            ? "unknown"
            : originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "images/" + UUID.randomUUID() + "-" + fileName;
    }

}