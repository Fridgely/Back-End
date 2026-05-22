package soon.fridgely.global.support.fake;

import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.global.support.image.ImageManager;

import java.util.ArrayList;
import java.util.List;

public class FakeImageManager extends ImageManager {

    private String uploadResult;
    private final List<String> deletedUrls = new ArrayList<>();

    public FakeImageManager() {
        super(null, null);
    }

    @Override
    public String upload(MultipartFile file) {
        return uploadResult;
    }

    @Override
    public void delete(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            deletedUrls.add(imageUrl);
        }
    }

    public void setUploadResult(String url) {
        this.uploadResult = url;
    }

    public List<String> getDeletedUrls() {
        return List.copyOf(deletedUrls);
    }

    public void reset() {
        uploadResult = null;
        deletedUrls.clear();
    }
}
