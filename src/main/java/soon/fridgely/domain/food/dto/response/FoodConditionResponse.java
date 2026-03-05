package soon.fridgely.domain.food.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.StorageType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "식재료 상태 정보")
public record FoodConditionResponse(

    @Schema(description = "유통기한", example = "2026-01-15T00:00:00")
    LocalDateTime expirationDate,

    @Schema(description = "보관 위치", example = "REFRIGERATOR")
    StorageType storageType,

    @Schema(description = "식재료 상태 (GREEN/YELLOW/RED/BLACK)", example = "GREEN")
    FoodStatus foodStatus,

    @Schema(description = "유통기한까지 남은 일수", example = "7")
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