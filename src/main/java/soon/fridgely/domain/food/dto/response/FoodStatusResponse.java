package soon.fridgely.domain.food.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.food.entity.FoodStatus;

import java.util.List;
import java.util.Map;

@Schema(description = "유통기한 상태별 식재료 목록")
public record FoodStatusResponse(

    @Schema(description = "유통기한 만료 식재료 목록")
    List<FoodResponse> black,

    @Schema(description = "유통기한 임박 (10일 이내) 식재료 목록")
    List<FoodResponse> red,

    @Schema(description = "유통기한 주의 (20일 이내) 식재료 목록")
    List<FoodResponse> yellow,

    @Schema(description = "유통기한 양호 식재료 목록")
    List<FoodResponse> green

) {
    public static FoodStatusResponse from(Map<FoodStatus, List<FoodResponse>> map) {
        return new FoodStatusResponse(
            map.getOrDefault(FoodStatus.BLACK, List.of()),
            map.getOrDefault(FoodStatus.RED, List.of()),
            map.getOrDefault(FoodStatus.YELLOW, List.of()),
            map.getOrDefault(FoodStatus.GREEN, List.of())
        );
    }
}