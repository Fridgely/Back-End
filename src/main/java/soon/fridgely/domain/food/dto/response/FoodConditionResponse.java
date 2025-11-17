package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.StorageType;

import java.time.LocalDateTime;

public record FoodConditionResponse(
    LocalDateTime expirationDate,
    StorageType storageType,
    FoodStatus foodStatus
) {
}