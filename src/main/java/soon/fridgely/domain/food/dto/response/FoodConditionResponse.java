package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.StorageType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FoodConditionResponse(
    LocalDateTime expirationDate,
    StorageType storageType,
    FoodStatus foodStatus,
    long daysLeft
) {

    public static FoodConditionResponse of(Food food, LocalDate now) {
        return new FoodConditionResponse(
            food.getExpirationDate(),
            food.getStorageType(),
            food.getFoodStatus(),
            food.calculateDaysLeft(now)
        );
    }

}