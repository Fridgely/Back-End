package soon.fridgely.domain.food.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityUnitTest {

    @Test
    void 수량을_생성하면_소수점_2자리로_반올림된다() {
        // given
        BigDecimal amount = new BigDecimal("1.567");

        // when
        Quantity quantity = Quantity.register(amount, Unit.KG);

        // then
        assertThat(quantity)
            .extracting("amount", "unit")
            .containsExactly(new BigDecimal("1.57"), Unit.KG);
    }

    @Test
    void 생성_시_amount가_null이면_0으로_설정된다() {
        // given
        Quantity quantity = Quantity.register(null, Unit.PIECE);

        // expected
        assertThat(quantity.amount()).isEqualTo(new BigDecimal("0"));
    }

    @Test
    void 동일한_단위의_수량을_더하면_합산된_수량이_반환된다() {
        // given
        Quantity quantity1 = new Quantity(new BigDecimal("1.50"), Unit.KG);
        Quantity quantity2 = new Quantity(new BigDecimal("2.30"), Unit.KG);

        // when
        Quantity result = quantity1.plus(quantity2);

        // then
        assertThat(result)
            .extracting("amount", "unit")
            .containsExactly(new BigDecimal("3.80"), Unit.KG);
    }

    @Test
    void 다른_단위의_수량을_더하면_예외가_발생한다() {
        // given
        Quantity quantity1 = new Quantity(new BigDecimal("1.00"), Unit.KG);
        Quantity quantity2 = new Quantity(new BigDecimal("500.00"), Unit.ML);

        // expected
        assertThatThrownBy(() -> quantity1.plus(quantity2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("동일한 단위끼리만 계산할 수 있습니다.");
    }

    @Test
    void 동일한_단위의_수량을_빼면_차감된_수량이_반환된다() {
        // given
        Quantity quantity1 = new Quantity(new BigDecimal("5.00"), Unit.KG);
        Quantity quantity2 = new Quantity(new BigDecimal("2.30"), Unit.KG);

        // when
        Quantity result = quantity1.minus(quantity2);

        // then
        assertThat(result)
            .extracting("amount", "unit")
            .containsExactly(new BigDecimal("2.70"), Unit.KG);
    }

    @Test
    void 다른_단위의_수량을_빼면_예외가_발생한다() {
        // given
        Quantity quantity1 = new Quantity(new BigDecimal("1.00"), Unit.KG);
        Quantity quantity2 = new Quantity(new BigDecimal("500.00"), Unit.ML);

        // expected
        assertThatThrownBy(() -> quantity1.minus(quantity2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("동일한 단위끼리만 계산할 수 있습니다.");
    }

    @Test
    void 재고보다_많은_양을_빼면_예외가_발생한다() {
        // given
        Quantity quantity1 = new Quantity(new BigDecimal("1.00"), Unit.KG);
        Quantity quantity2 = new Quantity(new BigDecimal("2.00"), Unit.KG);

        // expected
        assertThatThrownBy(() -> quantity1.minus(quantity2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("재고보다 많은 양을 소비할 수 없습니다.");
    }

    @Test
    void 동일한_수량을_빼면_0이_반환된다() {
        // given
        Quantity quantity1 = new Quantity(new BigDecimal("3.50"), Unit.PIECE);
        Quantity quantity2 = new Quantity(new BigDecimal("3.50"), Unit.PIECE);

        // when
        Quantity result = quantity1.minus(quantity2);

        // then
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.isZero()).isTrue();
    }

    @ParameterizedTest(name = "수량 {0}은 isZero가 {1}이어야 한다")
    @CsvSource({
        "0, true",
        "0.00, true",
        "0.01, false",
        "1.00, false",
        "-0.00, true"
    })
    void 수량이_0인지_판단한다(String amountStr, boolean expectedZero) {
        // given
        Quantity quantity = new Quantity(new BigDecimal(amountStr), Unit.KG);

        // expected
        assertThat(quantity.isZero()).isEqualTo(expectedZero);
    }

}