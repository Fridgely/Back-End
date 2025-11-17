package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.Food;

public record FoodResponse(

    long id,
    String name,
    String categoryName,
    String imageURL,
    QuantityResponse quantity,
    FoodConditionResponse condition

) {

    public static FoodResponse from(Food food) {
        return new FoodResponse(
            food.getId(),
            food.getName(),
            food.getCategory().getName(),
            food.getImageURL(),
            new QuantityResponse(food.getQuantity().amount(), food.getQuantity().unit()),
            new FoodConditionResponse(food.getExpirationDate(), food.getStorageType(), food.getFoodStatus())
        );
    }

}