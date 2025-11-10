package soon.fridgely.domain.food.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.entity.Food;

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

}