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

import java.time.LocalDateTime;
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
        @Param("memberId") long memberId,
        @Param("status") EntityStatus status
    );

    /*
     * 특정 회원의 재고가 0인 음식 조회
     */
    @Query("""
        SELECT f FROM Food f
        WHERE f.member.id = :memberId
        AND f.quantity.amount = 0
        AND f.status = :status
        """)
    List<Food> findAllOutOfStock(
        @Param("memberId") long memberId,
        @Param("status") EntityStatus status
    );

    /*
     * 회원이 소유한 Food 중 유통기한이 특정 기간 내에 속하는 Food 조회
     */
    @Query("""
            SELECT f FROM Food f
            JOIN f.refrigerator r
            JOIN MemberRefrigerator mr ON mr.refrigerator = r
            WHERE mr.member.id = :memberId
            AND mr.status = :status
            AND f.status = :status
            AND f.expirationDate BETWEEN :startDateTime AND :endDateTime
            ORDER BY f.expirationDate ASC
        """)
    List<Food> findMyFoodsExpiringBetween(
        @Param("memberId") long memberId,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        @Param("status") EntityStatus status
    );

    /*
     * 특정 냉장고의 Food를 ID로 조회
     */
    @Query("""
            SELECT f FROM Food f
            JOIN FETCH f.category c
            WHERE f.id = :foodId
            AND f.refrigerator.id = :refrigeratorId
            AND f.status = :status
        """)
    Optional<Food> findByIdAndRefrigeratorIdAndStatusWithCategory(
        @Param("foodId") long foodId,
        @Param("refrigeratorId") long refrigeratorId,
        @Param("status") EntityStatus status
    );

    /*
     * 특정 냉장고의 Food를 커서 기반 페이징으로 조회
     */
    @Query("""
            SELECT f FROM Food f
            JOIN FETCH f.category c
            WHERE f.refrigerator.id = :refrigeratorId
            AND f.id < :cursorId
            AND f.status = :status
            ORDER BY f.id DESC
        """)
    Slice<Food> findAllByRefrigeratorWithCategory(
        @Param("refrigeratorId") long refrigeratorId,
        @Param("cursorId") long cursorId,
        @Param("status") EntityStatus status,
        Pageable pageable
    );

    Optional<Food> findByIdAndRefrigeratorIdAndStatus(long foodId, long refrigeratorId, EntityStatus status);

    Optional<Food> findByIdAndRefrigeratorId(long foodId, long refrigeratorId);

}