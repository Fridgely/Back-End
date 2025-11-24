package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.Food;

import java.time.LocalDate;

public record FoodResponse(

    long id,
    String name,
    String categoryName,
    String imageURL,
    QuantityResponse quantity,
    FoodConditionResponse condition

) {

    public static FoodResponse of(Food food, LocalDate now) {
        return new FoodResponse(
            food.getId(),
            food.getName(),
            food.getCategory().getName(),
            food.getImageURL(),
            QuantityResponse.from(food.getQuantity()),
            FoodConditionResponse.of(food, now)
        );
    }

}