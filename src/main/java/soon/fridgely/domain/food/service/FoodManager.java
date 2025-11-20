package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.service.CategoryFinder;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class FoodManager {

    private static final String FALLBACK_CATEGORY_NAME = "기타";

    private final FoodRepository foodRepository;
    private final MemberRepository memberRepository;
    private final RefrigeratorRepository refrigeratorRepository;
    private final CategoryFinder categoryFinder;

    @Transactional
    public void moveAllFoodsToFallback(long refrigeratorId, long categoryId) {
        Category fallbackCategory = categoryFinder.findByName(FALLBACK_CATEGORY_NAME, refrigeratorId);
        Category targetCategory = categoryFinder.findByRefrigerator(categoryId, refrigeratorId);
        foodRepository.moveAllFoodsToFallbackCategory(targetCategory, fallbackCategory);
    }

    @Transactional
    public void createFood(FoodInfo info, MemberRefrigeratorKey key, long categoryId) {
        Member member = memberRepository.findByIdAndStatus(key.memberId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Refrigerator refrigerator = refrigeratorRepository.findByIdAndStatus(key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Category category = categoryFinder.findByRefrigerator(categoryId, key.refrigeratorId());

        Food food = info.toEntity(member, refrigerator, category, LocalDate.now());
        foodRepository.save(food);
    }

    @Transactional
    public void update(long foodId, FoodInfo updateInfo, MemberRefrigeratorKey key, long categoryId) {
        Food food = find(foodId, key.refrigeratorId());
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

    @Transactional(readOnly = true)
    public Food find(long foodId, long refrigeratorId) {
        return foodRepository.findByIdAndRefrigeratorIdAndStatus(foodId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    @Transactional(readOnly = true)
    public Slice<Food> findAll(long refrigeratorId, long cursorId, Pageable pageable) {
        return foodRepository.findByRefrigeratorIdAndIdLessThanAndStatusOrderByIdDesc(
            refrigeratorId,
            cursorId,
            EntityStatus.ACTIVE,
            pageable
        );
    }

    private boolean hasCategoryChanged(Food food, long newCategoryId) {
        return food.getCategory().getId() != newCategoryId;
    }

}