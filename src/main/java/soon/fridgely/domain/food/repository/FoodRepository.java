package soon.fridgely.domain.food.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long>, FoodRepositoryCustom {

    /**
     * 지정된 카테고리에 속한 모든 Food의 카테고리를 fallback('기타')으로 일괄 변경
     * 삭제된(Category.status == 'DELETED') 카테고리에 속한 Food도 함께 이동하여 데이터 정합성을 보장
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Food f SET f.category = :fallback WHERE f.category = :target")
    void moveAllFoodsToFallbackCategory(
        @Param("target") Category targetCategory,
        @Param("fallback") Category fallbackCategory
    );

    List<Food> findAllByRefrigeratorIdAndStatus(long refrigeratorId, EntityStatus status);

    Optional<Food> findByIdAndRefrigeratorIdAndStatus(long foodId, long refrigeratorId, EntityStatus status);

    Optional<Food> findByIdAndRefrigeratorId(long foodId, long refrigeratorId);

    /**
     * 날짜 범위에 해당하는 ACTIVE Food의 foodStatus를 일괄 변경
     * 이미 동일한 status인 경우 제외하여 불필요한 UPDATE 방지
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Food f
            SET f.foodStatus = :newStatus
            WHERE f.status = soon.fridgely.domain.EntityStatus.ACTIVE
              AND f.foodStatus <> :newStatus
              AND (:from IS NULL OR f.expirationDate >= :from)
              AND (:to IS NULL OR f.expirationDate < :to)
        """)
    int bulkUpdateFoodStatus(
        @Param("newStatus") FoodStatus newStatus,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

}