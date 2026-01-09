package soon.fridgely.domain.food.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StockActionType;
import soon.fridgely.domain.food.entity.Unit;

import java.math.BigDecimal;

@Schema(description = "식재료 재고 변경 요청")
public record FoodStockUpdateRequest(

    @Schema(description = "변경할 수량", example = "0.5")
    @Positive(message = "수량은 양수여야 합니다.")
    @NotNull(message = "수량은 필수입니다.")
    BigDecimal amount,

    @Schema(description = "단위", example = "L")
    @NotNull(message = "단위는 필수입니다.")
    Unit unit,

    @Schema(description = "재고 변경 유형 (ADD: 추가, CONSUME: 차감)", example = "CONSUME")
    @NotNull(message = "변경 유형은 필수입니다.")
    StockActionType action

) {

    public Quantity toQuantity() {
        return new Quantity(amount, unit);
    }

}