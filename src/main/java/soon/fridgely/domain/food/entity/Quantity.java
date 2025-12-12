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

    public Quantity plus(Quantity other) {
        requireNonNull(other, "더할 수량은 필수입니다.");

        if (this.unit != other.unit) {
            throw new IllegalArgumentException("동일한 단위끼리만 계산할 수 있습니다.");
        }

        BigDecimal result = this.amount.add(other.amount);
        return new Quantity(result, this.unit);
    }

    public Quantity minus(Quantity other) {
        requireNonNull(other, "차감할 수량은 필수입니다.");

        if (this.unit != other.unit) {
            throw new IllegalArgumentException("동일한 단위끼리만 계산할 수 있습니다.");
        }

        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("재고보다 많은 양을 소비할 수 없습니다.");
        }

        return new Quantity(result, this.unit);
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

}