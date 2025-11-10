package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.service.CategoryFinder;
import soon.fridgely.domain.food.repository.FoodRepository;

@RequiredArgsConstructor
@Component
public class FoodManager {

    private static final String FALLBACK_CATEGORY_NAME = "기타";

    private final FoodRepository foodRepository;
    private final CategoryFinder categoryFinder;

    @Transactional
    public void moveAllFoodsToFallback(long refrigeratorId, long categoryId) {
        Category fallbackCategory = categoryFinder.findByName(FALLBACK_CATEGORY_NAME, refrigeratorId);
        Category targetCategory = categoryFinder.findByRefrigerator(categoryId, refrigeratorId);
        foodRepository.moveAllFoodsToFallbackCategory(targetCategory, fallbackCategory);
    }

}