package soon.fridgely.domain.food.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Objects.requireNonNull;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quantity {

    @Column(name = "quantity_amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", nullable = false, length = 10)
    private Unit unit;

    private Quantity(BigDecimal amount, Unit unit) {
        requireNonNull(unit, "unit은 필수입니다.");

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }

        this.amount = amount;
        this.unit = unit;
    }

    public static Quantity register(BigDecimal amount, Unit unit) {
        return new Quantity(amount, unit);
    }

    public Quantity plus(Quantity other) {
        requireNonNull(other, "더할 수량은 필수입니다.");
        validateUnit(other);

        return new Quantity(this.amount.add(other.amount), this.unit);
    }

    public Quantity minus(Quantity other) {
        requireNonNull(other, "차감할 수량은 필수입니다.");
        validateUnit(other);

        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }

        return new Quantity(result, this.unit);
    }

    private void validateUnit(Quantity other) {
        if (this.unit != other.unit) {
            throw new CoreException(ErrorType.INVALID_REQUEST);
        }
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

}