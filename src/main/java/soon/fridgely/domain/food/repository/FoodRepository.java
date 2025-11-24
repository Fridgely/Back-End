package soon.fridgely.domain.food.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.entity.Food;

import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long> {

    /*
     * 지정된 카테고리에 속한 모든 Food의 카테고리를 fallback('기타')으로 일괄 변경
     * 삭제된(Category.status == 'DELETED') 카테고리에 속한 Food도 함께 이동하여 데이터 정합성을 보장
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Food f SET f.category = :fallback WHERE f.category = :target")
    void moveAllFoodsToFallbackCategory(
        @Param("target") Category targetCategory,
        @Param("fallback") Category fallbackCategory
    );

    /*
        * 특정 회원이 소유한 모든 Food 조회
     */
    @Query("""
            SELECT f FROM Food f
            JOIN FETCH f.category c
            JOIN f.refrigerator r
            JOIN MemberRefrigerator mr ON mr.refrigerator = r
            WHERE mr.member.id = :memberId
            AND mr.status = :status
            AND f.status = :status
            ORDER BY f.expirationDate ASC, f.id DESC
        """)
    List<Food> findAllMyFoods(
        @Param("memberId") Long memberId,
        @Param("status") EntityStatus status
    );
    Optional<Food> findByIdAndRefrigeratorIdAndStatus(long foodId, long refrigeratorId, EntityStatus status);

    Slice<Food> findByRefrigeratorIdAndIdLessThanAndStatusOrderByIdDesc(
        long refrigeratorId,
        long id, // 마지막으로 조회된 Food의 ID
        EntityStatus status,
        Pageable pageable
    );

}