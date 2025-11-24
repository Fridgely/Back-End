package soon.fridgely.domain.food.dto.response;

import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.Unit;

import java.math.BigDecimal;

public record QuantityResponse(
    BigDecimal amount,
    Unit unit
) {

    public static QuantityResponse from(Quantity quantity) {
        return new QuantityResponse(quantity.amount(), quantity.unit());
    }

}