package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class FoodFinder {

    private final FoodRepository foodRepository;

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

    @Transactional(readOnly = true)
    public List<Food> findAllMyFoods(long memberId) {
        return foodRepository.findAllMyFoods(memberId, EntityStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Food> findMyFoodsExpiringBetween(Long memberId, LocalDateTime start, LocalDateTime end) {
        return foodRepository.findMyFoodsExpiringBetween(memberId, start, end);
    }

}