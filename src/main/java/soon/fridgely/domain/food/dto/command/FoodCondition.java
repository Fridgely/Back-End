package soon.fridgely.domain.food.dto.command;

import soon.fridgely.domain.food.entity.StorageType;

import java.time.LocalDateTime;

public record FoodCondition(
    LocalDateTime expirationDate,
    StorageType storageType
) {
}