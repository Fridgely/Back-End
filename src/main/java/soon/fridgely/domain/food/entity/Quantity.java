package soon.fridgely.domain.food.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Objects.requireNonNull;

@Embeddable
public record Quantity(

    @Column(name = "quantity_amount", precision = 10, scale = 2)
    BigDecimal amount,

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", nullable = false, length = 10)
    Unit unit

) {

    public static Quantity register(BigDecimal amount, Unit unit) {
        return new Quantity(
            amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP),
            requireNonNull(unit, "unit은 필수입니다.")
        );
    }

}