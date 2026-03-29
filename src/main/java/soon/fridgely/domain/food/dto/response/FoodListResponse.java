package soon.fridgely.domain.food.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.food.entity.Food;

import java.time.LocalDate;

@Schema(description = "식재료 목록 조회 응답")
public record FoodListResponse(

    @Schema(description = "냉장고 ID", example = "1")
    long refrigeratorId,

    @Schema(description = "식재료 ID", example = "1")
    long id,

    @Schema(description = "식재료 이름", example = "우유")
    String name,

    @Schema(description = "카테고리 이름", example = "유제품")
    String categoryName,

    @Schema(description = "이미지 URL", example = "https://example.com/images/milk.jpg")
    String imageURL,

    @Schema(description = "수량 정보")
    QuantityResponse quantity,

    @Schema(description = "상태 정보")
    FoodConditionResponse condition

) {

    public static FoodListResponse of(Food food, LocalDate now) {
        return new FoodListResponse(
            food.refrigeratorId(),
            food.getId(),
            food.getName(),
            food.getCategory().getName(),
            food.getImageURL(),
            QuantityResponse.from(food.getQuantity()),
            FoodConditionResponse.of(food, now)
        );
    }

}