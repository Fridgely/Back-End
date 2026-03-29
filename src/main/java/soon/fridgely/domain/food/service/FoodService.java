package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodCursorPageRequest;
import soon.fridgely.domain.food.dto.request.FoodStockUpdateRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodListResponse;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;
import soon.fridgely.global.support.image.ImageManager;
import soon.fridgely.global.support.logging.SlackMarkers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class FoodService {

    private final FoodFinder foodFinder;
    private final FoodManager foodManager;
    private final FoodModifier foodModifier;
    private final FoodRemover foodRemover;
    private final ImageManager imageManager;

    @ValidateRefrigeratorAccess(key = "#key")
    public void createFood(FoodCreateRequest request, MultipartFile file, MemberRefrigeratorKey key) {
        String uploadedUrl = imageManager.upload(file);

        try {
            foodManager.createFood(request.toFoodInfo(uploadedUrl), key, request.categoryId());
        } catch (Exception e) {
            rollbackImageUpload(uploadedUrl);
            throw e;
        }
    }

    @ValidateRefrigeratorAccess(key = "#key")
    public void updateFood(long foodId, FoodUpdateRequest request, MultipartFile file, MemberRefrigeratorKey key) {
        String uploadedUrl = file != null && !file.isEmpty()
            ? imageManager.upload(file)
            : null;

        try {
            foodModifier.update(foodId, request.toFoodInfo(uploadedUrl), key, request.categoryId());
        } catch (Exception e) {
            rollbackImageUpload(uploadedUrl);
            throw e;
        }
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public FoodDetailResponse findFood(long foodId, MemberRefrigeratorKey key) {
        Food found = foodFinder.find(foodId, key.refrigeratorId());
        LocalDate now = LocalDate.now();
        return FoodDetailResponse.of(found, now);
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public Slice<FoodListResponse> findAllFoods(MemberRefrigeratorKey key, FoodCursorPageRequest request) {
        LocalDate now = LocalDate.now();
        return foodFinder.findAll(
                key.refrigeratorId(),
                request.getCursorId(),
                request.toPageable(),
                request.getSortBy(),
                request.storageType()
            )
            .map(food -> FoodListResponse.of(food, now));
    }

    @Transactional(readOnly = true)
    public FoodStatusResponse findAllMyFoodsGroupedByStatus(long memberId) {
        List<Food> allFoods = foodFinder.findAllMyFoods(memberId);
        LocalDate now = LocalDate.now();

        Map<FoodStatus, List<FoodResponse>> groupedMap = allFoods.stream()
            .map(food -> FoodResponse.of(food, now))
            .collect(Collectors.groupingBy(response ->
                response.condition().foodStatus() // 음식 상태별로 그룹화
            ));

        return FoodStatusResponse.from(groupedMap);
    }

    @ValidateRefrigeratorAccess(key = "#key")
    public void deleteFood(long foodId, MemberRefrigeratorKey key) {
        foodRemover.remove(foodId, key.refrigeratorId());
    }

    @ValidateRefrigeratorAccess(key = "#key")
    public void updateFoodStock(long foodId, FoodStockUpdateRequest request, MemberRefrigeratorKey key) {
        Quantity quantity = request.toQuantity();

        switch (request.action()) {
            case ADD -> foodModifier.add(foodId, key.refrigeratorId(), quantity);
            case CONSUME -> foodModifier.consume(foodId, key.refrigeratorId(), quantity);
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