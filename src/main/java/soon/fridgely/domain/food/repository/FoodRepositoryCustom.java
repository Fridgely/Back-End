package soon.fridgely.domain.food.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.entity.StorageType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FoodRepositoryCustom {

    /**
     * 특정 냉장고의 음식을 동적 조건과 정렬로 조회
     *
     * @param refrigeratorId 냉장고 ID
     * @param cursorId       커서 ID
     * @param sortType       정렬 타입 (EXPIRATION, CREATED, NAME)
     * @param storageType    저장 위치 필터 (nullable)
     * @param pageable       페이지 정보
     * @return 음식 목록 (Slice)
     */
    Slice<Food> findAllDynamic(
        long refrigeratorId,
        long cursorId,
        FoodSortType sortType,
        StorageType storageType,
        Pageable pageable
    );

    /**
     * 특정 회원이 소유한 모든 음식 조회 (유통기한 오름차순)
     */
    List<Food> findAllMyFoods(long memberId);

    /**
     * 특정 회원의 음식 중 지정된 날짜 범위에 만료되는 음식 조회
     */
    List<Food> findMyFoodsExpiringBetween(
        long memberId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    );

    /**
     * 특정 회원의 재고가 0인 음식 조회
     */
    List<Food> findAllOutOfStock(long memberId);

    /**
     * 특정 냉장고의 음식을 ID로 조회
     */
    Optional<Food> findByIdAndRefrigeratorIdWithCategory(long foodId, long refrigeratorId);

}