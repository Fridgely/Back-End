package soon.fridgely.global.support.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.global.infra.provider.StorageProvider;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ImageManager {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

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
            throw new CoreException(ErrorType.STORAGE_UPLOAD_FAILED, "key: " + key);
        }

    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "contentType: " + contentType);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "fileSize: " + file.getSize());
        }
    }

    private String generateKey(String originalName) {
        String fileName = (originalName == null || originalName.isBlank())
            ? "unknown"
            : originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "images/" + UUID.randomUUID() + "-" + fileName;
    }

}