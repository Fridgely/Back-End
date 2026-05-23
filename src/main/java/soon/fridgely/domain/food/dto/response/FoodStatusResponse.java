package soon.fridgely.domain.food.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "유통기한 상태별 식재료 목록")
public record FoodStatusResponse(

    @Schema(description = "유통기한 만료 식재료 목록")
    List<FoodResponse> black,

    @Schema(description = "유통기한 만료 식재료 개수", example = "3")
    int blackCount,

    @Schema(description = "유통기한 임박 (10일 이내) 식재료 목록")
    List<FoodResponse> red,

    @Schema(description = "유통기한 임박 식재료 개수", example = "5")
    int redCount,

    @Schema(description = "유통기한 주의 (20일 이내) 식재료 목록")
    List<FoodResponse> yellow,

    @Schema(description = "유통기한 주의 식재료 개수", example = "2")
    int yellowCount,

    @Schema(description = "유통기한 양호 식재료 목록")
    List<FoodResponse> green,

    @Schema(description = "유통기한 양호 식재료 개수", example = "10")
    int greenCount

) {
}