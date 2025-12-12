package soon.fridgely.domain.food.dto.request;

import jakarta.validation.constraints.NotNull;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.StockActionType;
import soon.fridgely.domain.food.entity.Unit;

import java.math.BigDecimal;

public record FoodStockUpdateRequest(

    @NotNull(message = "수량은 필수입니다.")
    BigDecimal amount,

    @NotNull(message = "단위는 필수입니다.")
    Unit unit,

    @NotNull(message = "변경 유형은 필수입니다.")
    StockActionType action

) {

    public Quantity toQuantity() {
        return new Quantity(amount, unit);
    }

}