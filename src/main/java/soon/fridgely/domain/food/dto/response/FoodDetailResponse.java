package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FoodDetailResponse(

    long id,
    String name,
    String categoryName,
    BigDecimal amount,
    Unit unit,
    LocalDateTime expirationDate,
    String storageType,
    String foodStatus,
    String description,
    String imageURL

) {
    public static FoodDetailResponse from(Food food) {
        return new FoodDetailResponse(
            food.getId(),
            food.getName(),
            food.getCategory().getName(),
            food.getQuantity().amount(),
            food.getQuantity().unit(),
            food.getExpirationDate(),
            food.getStorageType().name(),
            food.getFoodStatus().name(),
            food.getDescription(),
            food.getImageURL()
        );
    }

}