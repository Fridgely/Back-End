package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.service.CategoryFinder;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class FoodModifier {

    private static final String FALLBACK_CATEGORY_NAME = "기타";

    private final FoodRepository foodRepository;
    private final CategoryFinder categoryFinder;

    @Transactional
    public void moveAllFoodsToFallback(long refrigeratorId, long categoryId) {
        Category fallbackCategory = categoryFinder.findByName(FALLBACK_CATEGORY_NAME, refrigeratorId);
        Category targetCategory = categoryFinder.findByRefrigerator(categoryId, refrigeratorId);
        foodRepository.moveAllFoodsToFallbackCategory(targetCategory, fallbackCategory);
    }

    @Transactional
    public void update(long foodId, FoodInfo updateInfo, MemberRefrigeratorKey key, long categoryId) {
        Food food = foodRepository.findByIdAndRefrigeratorIdAndStatus(foodId, key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Category category = null;
        if (hasCategoryChanged(food, categoryId)) {
            category = categoryFinder.findByRefrigerator(categoryId, key.refrigeratorId());
        }

        LocalDate now = LocalDate.now();
        food.update(
            updateInfo.name(),
            category,
            updateInfo.quantity(),
            updateInfo.condition().expirationDate(),
            updateInfo.condition().storageType(),
            updateInfo.description(),
            updateInfo.imageURL(),
            now
        );
    }

    private boolean hasCategoryChanged(Food food, long newCategoryId) {
        return food.getCategory().getId() != newCategoryId;
    }

}