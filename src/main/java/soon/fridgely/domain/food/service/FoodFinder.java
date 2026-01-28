package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.utils.TimeRangeUtils;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class FoodFinder {

    private final FoodRepository foodRepository;

    @Transactional(readOnly = true)
    public Food find(long foodId, long refrigeratorId) {
        return foodRepository.findByIdAndRefrigeratorIdAndStatusWithCategory(foodId, refrigeratorId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    @Transactional(readOnly = true)
    public Slice<Food> findAll(long refrigeratorId, long cursorId, Pageable pageable, FoodSortType sortType) {
        return switch (sortType) {
            case EXPIRATION -> foodRepository.findAllByRefrigeratorOrderByExpiration(
                refrigeratorId,
                cursorId,
                EntityStatus.ACTIVE,
                pageable
            );
            case CREATED -> foodRepository.findAllByRefrigeratorOrderByCreated(
                refrigeratorId,
                cursorId,
                EntityStatus.ACTIVE,
                pageable
            );
            case NAME -> foodRepository.findAllByRefrigeratorOrderByName(
                refrigeratorId,
                cursorId,
                EntityStatus.ACTIVE,
                pageable
            );
        };
    }

    @Transactional(readOnly = true)
    public List<Food> findAllMyFoods(long memberId) {
        return foodRepository.findAllMyFoods(memberId, EntityStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Food> findMyFoodsExpiringOnDate(long memberId, LocalDate targetDate) {
        return foodRepository.findMyFoodsExpiringBetween(
            memberId,
            TimeRangeUtils.startOfDay(targetDate),
            TimeRangeUtils.endOfDay(targetDate),
            EntityStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public List<Food> findAllOutOfStock(long memberId) {
        return foodRepository.findAllOutOfStock(memberId, EntityStatus.ACTIVE);
    }

}