package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.Food;

public record FoodDetailResponse(

    long id,
    String name,
    String categoryName,
    QuantityResponse quantity,
    FoodConditionResponse condition,
    String description,
    String imageURL

) {

    public static FoodDetailResponse from(Food food) {
        return new FoodDetailResponse(
            food.getId(),
            food.getName(),
            food.getCategory().getName(),
            new QuantityResponse(food.getQuantity().amount(), food.getQuantity().unit()),
            new FoodConditionResponse(food.getExpirationDate(), food.getStorageType(), food.getFoodStatus()),
            food.getDescription(),
            food.getImageURL()
        );
    }

}