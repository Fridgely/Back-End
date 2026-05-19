package soon.fridgely.domain.food.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.service.FoodService;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.support.image.ImageManager;
import soon.fridgely.global.support.logging.SlackMarkers;

@Slf4j
@RequiredArgsConstructor
@Component
public class FoodFacade {

    private final FoodService foodService;
    private final ImageManager imageManager;

    public void createFood(FoodCreateRequest request, MultipartFile file, MemberRefrigeratorKey key) {
        String uploadedUrl = file != null && !file.isEmpty()
            ? imageManager.upload(file)
            : null;
        try {
            foodService.createFood(request.toFoodInfo(uploadedUrl), key, request.categoryId());
        } catch (Exception e) {
            rollbackImageUpload(uploadedUrl);
            throw e;
        }
    }

    public void updateFood(long foodId, FoodUpdateRequest request, MultipartFile file, MemberRefrigeratorKey key) {
        String uploadedUrl = file != null && !file.isEmpty()
            ? imageManager.upload(file)
            : null;
        try {
            foodService.updateFood(foodId, request.toFoodInfo(uploadedUrl), key, request.categoryId());
        } catch (Exception e) {
            rollbackImageUpload(uploadedUrl);
            throw e;
        }
    }

    private void rollbackImageUpload(String imageUrl) {
        if (imageUrl != null) {
            try {
                imageManager.delete(imageUrl);
            } catch (Exception e) {
                log.warn(SlackMarkers.SYSTEM, "[Food] 이미지 롤백 실패 - 수동 정리 필요 (ImageUrl={})", imageUrl, e);
            }
        }
    }
}
