package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.FoodStatus;

import java.util.List;
import java.util.Map;

public record FoodStatusResponse(
    List<FoodResponse> black,
    List<FoodResponse> red,
    List<FoodResponse> yellow,
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