package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodStatus;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FoodsByStatus {

    private final Map<FoodStatus, List<FoodResponse>> map;

    private FoodsByStatus(Map<FoodStatus, List<FoodResponse>> map) {
        Map<FoodStatus, List<FoodResponse>> copy = new HashMap<>();
        map.forEach((status, foods) -> copy.put(status, Collections.unmodifiableList(foods)));
        this.map = Collections.unmodifiableMap(copy);
    }

    public static FoodsByStatus of(List<Food> foods, LocalDate now) {
        Map<FoodStatus, List<FoodResponse>> grouped = foods.stream()
            .map(food -> FoodResponse.of(food, now))
            .collect(Collectors.groupingBy(response -> response.condition().foodStatus()));
        return new FoodsByStatus(grouped);
    }

    public List<FoodResponse> get(FoodStatus status) {
        return map.getOrDefault(status, List.of());
    }

    public FoodStatusResponse toStatusResponse() {
        List<FoodResponse> black = get(FoodStatus.BLACK);
        List<FoodResponse> red = get(FoodStatus.RED);
        List<FoodResponse> yellow = get(FoodStatus.YELLOW);
        List<FoodResponse> green = get(FoodStatus.GREEN);

        return new FoodStatusResponse(
            black, black.size(),
            red, red.size(),
            yellow, yellow.size(),
            green, green.size()
        );
    }
}
