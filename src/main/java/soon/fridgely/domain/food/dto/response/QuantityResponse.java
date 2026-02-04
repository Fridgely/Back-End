package soon.fridgely.domain.food.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.food.entity.Quantity;
import soon.fridgely.domain.food.entity.Unit;

import java.math.BigDecimal;

@Schema(description = "수량 정보")
public record QuantityResponse(

    @Schema(description = "수량", example = "1.5")
    BigDecimal amount,

    @Schema(description = "단위", example = "L")
    Unit unit

) {

    public static QuantityResponse from(Quantity quantity) {
        return new QuantityResponse(quantity.getAmount(), quantity.getUnit());
    }

}