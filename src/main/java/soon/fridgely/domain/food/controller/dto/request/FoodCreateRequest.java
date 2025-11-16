package soon.fridgely.domain.food.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import soon.fridgely.domain.food.dto.FoodInfo;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FoodCreateRequest(

    @NotBlank(message = "음식 이름은 필수입니다.")
    String name,

    @Positive(message = "카테고리 ID는 양수여야 합니다.")
    @NotNull(message = "카테고리 ID는 필수입니다.")
    long categoryId,

    @Min(value = 0, message = "수량은 0 이상이어야 합니다.")
    @NotNull(message = "수량은 필수입니다.")
    BigDecimal amount,

    @NotNull(message = "단위는 필수입니다.")
    Unit unit,

    @NotNull(message = "유통기한은 필수입니다.")
    LocalDateTime expirationDate,

    @NotNull(message = "보관 위치는 필수입니다.")
    StorageType storageType,

    String description

) {

    public FoodInfo toFoodInfo(LocalDate now, String imageURL) {
        return new FoodInfo(
            name,
            categoryId,
            Quantity.register(amount, unit),
            expirationDate,
            storageType,
            FoodStatus.fromDaysLeft(LocalDate.from(expirationDate), now),
            description,
            imageURL
        );
    }

}