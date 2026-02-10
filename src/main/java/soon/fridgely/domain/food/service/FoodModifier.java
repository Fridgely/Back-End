package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.service.CategoryFinder;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.image.event.ImageDeleteEvent;

import java.time.LocalDate;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class FoodModifier {

    private static final String FALLBACK_CATEGORY_NAME = "기타";

    private final FoodRepository foodRepository;
    private final CategoryFinder categoryFinder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void moveAllFoodsToFallback(long refrigeratorId, long categoryId) {
        Category fallbackCategory = categoryFinder.findByName(FALLBACK_CATEGORY_NAME, refrigeratorId);
        Category targetCategory = categoryFinder.findByRefrigerator(categoryId, refrigeratorId);
        foodRepository.moveAllFoodsToFallbackCategory(targetCategory, fallbackCategory);
    }

    /**
     * 음식 정보 수정
     */
    @Transactional
    public void update(long foodId, FoodInfo updateInfo, MemberRefrigeratorKey key, long categoryId) {
        Food food = findFood(foodId, key.refrigeratorId());
        Category category = hasCategoryChanged(food, categoryId)
            ? categoryFinder.findByRefrigerator(categoryId, key.refrigeratorId())
            : null;

        deleteOldImageIfChanged(food.getImageURL(), updateInfo.imageURL());

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

    /**
     * 음식 수량 추가
     */
    @Transactional
    public void add(long foodId, long refrigeratorId, Quantity amount) {
        Food food = findFood(foodId, refrigeratorId);
        food.add(amount);
    }

    /**
     * 음식 수량 소비
     */
    @Transactional
    public void consume(long foodId, long refrigeratorId, Quantity amount) {
        Food food = findFood(foodId, refrigeratorId);
        food.consume(amount);
    }

    private Food findFood(long foodId, long refrigeratorId) {
        return foodRepository.findByIdAndRefrigeratorIdAndStatus(foodId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    private void deleteOldImageIfChanged(String oldImageUrl, String newImageUrl) {
        if (!Objects.equals(oldImageUrl, newImageUrl) && oldImageUrl != null) {
            eventPublisher.publishEvent(new ImageDeleteEvent(oldImageUrl));
        }
    }

    private boolean hasCategoryChanged(Food food, long newCategoryId) {
        return food.getCategory().getId() != newCategoryId;
    }

}