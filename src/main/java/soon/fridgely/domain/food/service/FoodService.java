package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;
import soon.fridgely.global.support.CursorPageRequest;
import soon.fridgely.global.support.image.ImageManager;

@RequiredArgsConstructor
@Service
public class FoodService {

    private final FoodManager foodManager;
    private final ImageManager imageManager;

    private final RefrigeratorAccessValidator refrigeratorAccessValidator;

    public void createFood(FoodCreateRequest request, MultipartFile file, MemberRefrigeratorKey key) {
        refrigeratorAccessValidator.validateMembership(key);

        String uploadedUrl = imageManager.upload(file);
        foodManager.createFood(
            request.toFoodInfo(uploadedUrl),
            key,
            request.categoryId()
        );
    }

    @Transactional(readOnly = true)
    public FoodDetailResponse findFood(long foodId, MemberRefrigeratorKey key) {
        refrigeratorAccessValidator.validateMembership(key);

        Food found = foodManager.find(foodId, key.refrigeratorId());
        return FoodDetailResponse.from(found);
    }

    @Transactional(readOnly = true)
    public Slice<FoodResponse> findAllFoods(MemberRefrigeratorKey key, CursorPageRequest request) {
        refrigeratorAccessValidator.validateMembership(key);

        return foodManager.findAll(key.refrigeratorId(), request.getCursorId(), request.toPageable())
            .map(FoodResponse::from);
    }

}