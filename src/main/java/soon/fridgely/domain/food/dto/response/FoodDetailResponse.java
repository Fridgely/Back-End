package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.Food;

import java.time.LocalDate;

public record FoodDetailResponse(

    long id,
    String name,
    String categoryName,
    QuantityResponse quantity,
    FoodConditionResponse condition,
    String description,
    String imageURL

) {

    public static FoodDetailResponse of(Food food, LocalDate now) {
        return new FoodDetailResponse(
            food.getId(),
            food.getName(),
            food.getCategory().getName(),
            QuantityResponse.from(food.getQuantity()),
            FoodConditionResponse.of(food, now),
            food.getDescription(),
            food.getImageURL()
        );
    }

}