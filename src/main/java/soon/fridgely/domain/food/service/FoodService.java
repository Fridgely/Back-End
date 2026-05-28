package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.dto.request.FoodStockUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodListResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.dto.response.FoodsByStatus;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;
import soon.fridgely.global.support.CursorPageRequest;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.image.event.ImageDeleteEvent;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FoodService {

    private static final String FALLBACK_CATEGORY_NAME = "기타";

    private final FoodRepository foodRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final RefrigeratorRepository refrigeratorRepository;
    private final ApplicationEventPublisher eventPublisher;

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional
    public void createFood(FoodInfo info, MemberRefrigeratorKey key, long categoryId) {
        Member member = memberRepository.findByIdAndStatus(key.memberId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Refrigerator refrigerator = refrigeratorRepository.findByIdAndStatus(key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Category category = categoryRepository.findByIdAndRefrigeratorIdAndStatus(categoryId, key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        foodRepository.save(info.toEntity(member, refrigerator, category, LocalDate.now()));
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional
    public void updateFood(long foodId, FoodInfo updateInfo, MemberRefrigeratorKey key, long categoryId) {
        Food food = foodRepository.findByIdAndRefrigeratorIdAndStatus(foodId, key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Category category = food.isCategoryDifferent(categoryId)
            ? categoryRepository.findByIdAndRefrigeratorIdAndStatus(categoryId, key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA))
            : null;

        String newImageUrl = updateInfo.imageURL();
        if (newImageUrl != null && food.isImageChangedFrom(newImageUrl)) {
            eventPublisher.publishEvent(new ImageDeleteEvent(food.getImageURL()));
        }

        food.update(
            updateInfo.name(),
            category,
            updateInfo.quantity(),
            updateInfo.condition().expirationDate(),
            updateInfo.condition().storageType(),
            updateInfo.description(),
            (newImageUrl != null) ? newImageUrl : food.getImageURL(),
            LocalDate.now()
        );
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public FoodDetailResponse findFood(long foodId, MemberRefrigeratorKey key) {
        Food found = foodRepository.findByIdAndRefrigeratorIdWithCategory(foodId, key.refrigeratorId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        return FoodDetailResponse.of(found, LocalDate.now());
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional(readOnly = true)
    public Slice<FoodListResponse> findAllFoods(MemberRefrigeratorKey key, CursorPageRequest<FoodSortType> request, StorageType storageType) {
        LocalDate now = LocalDate.now();
        FoodSortType sortBy = request.getSortBy() != null ? request.getSortBy() : FoodSortType.EXPIRATION;
        return foodRepository.findAllDynamic(
                key.refrigeratorId(),
                request.getCursorId(),
                sortBy,
                storageType,
                request.toPageable()
            )
            .map(food -> FoodListResponse.of(food, now));
    }

    @Transactional(readOnly = true)
    public FoodStatusResponse findAllMyFoodsGroupedByStatus(long memberId) {
        List<Food> allFoods = foodRepository.findAllMyFoods(memberId);
        return FoodsByStatus.of(allFoods, LocalDate.now()).toStatusResponse();
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional
    public void deleteFood(long foodId, MemberRefrigeratorKey key) {
        Food food = foodRepository.findByIdAndRefrigeratorId(foodId, key.refrigeratorId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        if (food.isDeleted()) {
            return;
        }
        publishImageDeleteEventIfPresent(food);
        food.delete();
    }

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional
    public void updateFoodStock(long foodId, FoodStockUpdateRequest request, MemberRefrigeratorKey key) {
        Quantity quantity = request.toQuantity();
        switch (request.action()) {
            case ADD -> addStock(foodId, key.refrigeratorId(), quantity);
            case CONSUME -> consumeStock(foodId, key.refrigeratorId(), quantity);
        }
    }

    @Transactional
    public void removeAllByRefrigeratorId(long refrigeratorId) {
        List<Food> foods = foodRepository.findAllByRefrigeratorIdAndStatus(refrigeratorId, EntityStatus.ACTIVE);
        for (Food food : foods) {
            publishImageDeleteEventIfPresent(food);
            food.delete();
        }
    }

    @Transactional
    public void moveAllFoodsToFallback(long refrigeratorId, long categoryId) {
        Category fallbackCategory = categoryRepository.findByNameAndRefrigeratorIdAndStatus(
                FALLBACK_CATEGORY_NAME, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Category targetCategory = categoryRepository.findByIdAndRefrigeratorIdAndStatus(
                categoryId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        foodRepository.moveAllFoodsToFallbackCategory(targetCategory, fallbackCategory);
    }

    private void addStock(long foodId, long refrigeratorId, Quantity amount) {
        Food food = foodRepository.findByIdAndRefrigeratorIdAndStatus(foodId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        food.add(amount);
    }

    private void consumeStock(long foodId, long refrigeratorId, Quantity amount) {
        Food food = foodRepository.findByIdAndRefrigeratorIdAndStatus(foodId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        food.consume(amount);
    }

    private void publishImageDeleteEventIfPresent(Food food) {
        if (food.hasImage()) {
            eventPublisher.publishEvent(new ImageDeleteEvent(food.getImageURL()));
        }
    }
}
