package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.entity.StorageType;
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
        return foodRepository.findByIdAndRefrigeratorIdWithCategory(foodId, refrigeratorId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    /**
     * 특정 냉장고의 음식 목록을 조회
     *
     * @param refrigeratorId 냉장고 ID
     * @param cursorId       커서 ID (페이지네이션)
     * @param pageable       페이지 정보
     * @param sortType       정렬 타입 (EXPIRATION, CREATED, NAME)
     * @param storageType    저장 위치 필터 (nullable)
     * @return 음식 목록 (Slice)
     */
    @Transactional(readOnly = true)
    public Slice<Food> findAll(
        long refrigeratorId,
        long cursorId,
        Pageable pageable,
        FoodSortType sortType,
        StorageType storageType
    ) {
        return foodRepository.findAllDynamic(
            refrigeratorId,
            cursorId,
            sortType,
            storageType,
            pageable
        );
    }

    @Transactional(readOnly = true)
    public List<Food> findAllMyFoods(long memberId) {
        return foodRepository.findAllMyFoods(memberId);
    }

    @Transactional(readOnly = true)
    public List<Food> findMyFoodsExpiringOnDate(long memberId, LocalDate targetDate) {
        return foodRepository.findMyFoodsExpiringBetween(
            memberId,
            TimeRangeUtils.startOfDay(targetDate),
            TimeRangeUtils.endOfDay(targetDate)
        );
    }

    @Transactional(readOnly = true)
    public List<Food> findAllOutOfStock(long memberId) {
        return foodRepository.findAllOutOfStock(memberId);
    }

}