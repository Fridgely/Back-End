package soon.fridgely.domain.food.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import soon.fridgely.domain.food.dto.command.FoodCondition;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "식재료 수정 요청")
public record FoodUpdateRequest(

    @Schema(description = "식재료 이름", example = "우유")
    @NotBlank(message = "음식 이름은 필수입니다.")
    String name,

    @Schema(description = "카테고리 ID", example = "1")
    @Positive(message = "카테고리 ID는 양수여야 합니다.")
    @NotNull(message = "카테고리 ID는 필수입니다.")
    long categoryId,

    @Schema(description = "수량", example = "1.5")
    @Min(value = 0, message = "수량은 0 이상이어야 합니다.")
    @NotNull(message = "수량은 필수입니다.")
    BigDecimal amount,

    @Schema(description = "단위", example = "L")
    @NotNull(message = "단위는 필수입니다.")
    Unit unit,

    @Schema(description = "유통기한", example = "2026-01-15T00:00:00")
    @NotNull(message = "유통기한은 필수입니다.")
    LocalDateTime expirationDate,

    @Schema(description = "보관 위치", example = "FROZEN")
    @NotNull(message = "보관 위치는 필수입니다.")
    StorageType storageType,

    @Schema(description = "메모", example = "유기농 우유")
    String description

) {

    public FoodInfo toFoodInfo(String imageURL) {
        return new FoodInfo(
            name,
            Quantity.register(amount, unit),
            toFoodCondition(),
            description,
            imageURL
        );
    }

    private FoodCondition toFoodCondition() {
        return new FoodCondition(
            expirationDate,
            storageType
        );
    }

}