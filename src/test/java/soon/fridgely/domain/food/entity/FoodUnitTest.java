package soon.fridgely.domain.food.entity;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static soon.fridgely.global.support.fixture.FoodFixture.food;

class FoodUnitTest {

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    private Member member;
    private Refrigerator refrigerator;
    private Category category;

    @BeforeEach
    void setUp() {
        member = fixtureMonkey.giveMeOne(Member.class);
        refrigerator = fixtureMonkey.giveMeOne(Refrigerator.class);
        category = fixtureMonkey.giveMeOne(Category.class);
    }

    @Test
    void 음식을_등록하면_입력받은_기준_시간에_따라_상태가_자동으로_설정되어야_한다() {
        // given
        var quantity = fixtureMonkey.giveMeOne(Quantity.class);
        LocalDate fixedNow = LocalDate.of(2025, 1, 1);
        LocalDateTime expirationDate = fixedNow.plusDays(5).atStartOfDay();

        // when
        Food food = Food.register(
            refrigerator,
            member,
            "시들한 당근",
            category,
            quantity,
            expirationDate,
            StorageType.REFRIGERATION,
            "설명",
            "http://image.url",
            fixedNow
        );

        // then
        assertThat(food.getName()).isEqualTo("시들한 당근");
        assertThat(food.getExpirationDate()).isEqualTo(expirationDate);
        assertThat(food.getFoodStatus()).isEqualTo(FoodStatus.RED);
    }

    @Test
    void 음식_등록_시_필수_값이_누락되면_예외가_발생한다() {
        // given
        LocalDate now = LocalDate.now();

        // expected
        assertThatThrownBy(() -> Food.register(
            refrigerator,
            member,
            "이름",
            category,
            new Quantity(BigDecimal.ONE, Unit.PIECE),
            null,
            StorageType.REFRIGERATION,
            "설명",
            "url",
            now
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("expirationDate는 필수입니다.");
    }

    @Test
    void 음식_정보를_수정하면_필드값이_변경되고_날짜_변경_시_상태가_재계산되어야_한다() {
        // given
        LocalDate fixedNow = LocalDate.of(2025, 1, 1);
        LocalDateTime oldExpirationDate = fixedNow.minusDays(1).atStartOfDay();

        Food food = food(fixtureMonkey, refrigerator, member, category)
            .set("expirationDate", oldExpirationDate)
            .set("foodStatus", FoodStatus.BLACK)
            .sample();

        // 유통기간 30일 후로 변경 -> 상태가 GREEN으로 변경되어야 함
        LocalDateTime newExpirationDate = fixedNow.plusDays(30).atStartOfDay();
        String newName = "신선한 우유";

        // when
        food.update(
            newName,
            category,
            fixtureMonkey.giveMeOne(Quantity.class),
            newExpirationDate,
            StorageType.FROZEN,
            "새로운 설명",
            "new_image.jpg",
            fixedNow
        );

        // then
        assertThat(food)
            .extracting("name", "expirationDate", "storageType", "foodStatus")
            .containsExactly(newName, newExpirationDate, StorageType.FROZEN, FoodStatus.GREEN);
    }

    @Test
    void 수정_시_카테고리와_이미지가_null이면_기존_값을_유지한다() {
        // given
        LocalDate fixedNow = LocalDate.of(2025, 1, 1);
        String originalImage = "http://example.com/image.jpg";

        Food food = food(fixtureMonkey, refrigerator, member, category)
            .set("imageURL", originalImage)
            .sample();

        Category oldCategory = food.getCategory();

        // when
        food.update(
            "이름 변경",
            null,
            food.getQuantity(),
            food.getExpirationDate(),
            food.getStorageType(),
            "설명 변경",
            null,
            fixedNow
        );

        // then
        assertThat(food)
            .extracting("category", "imageURL")
            .containsExactly(oldCategory, originalImage);
    }

    @Test
    void 수정_시_필수_값이_누락되면_예외가_발생한다() {
        // given
        LocalDate fixedNow = LocalDate.of(2025, 1, 1);

        Food food = food(fixtureMonkey, refrigerator, member, category).sample();

        // expected
        assertThatThrownBy(() -> food.update(
            null,
            category,
            food.getQuantity(),
            food.getExpirationDate(),
            food.getStorageType(),
            "설명",
            "img.jpg",
            fixedNow
        ))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("name은 필수입니다.");
    }

    @ParameterizedTest(name = "기준일: {0}, 만료일: {1} -> 남은 일수: {2}")
    @CsvSource({
        "2025-01-01, 2025-01-04, 3", // 미래: 3일 남음
        "2025-01-01, 2025-01-02, 1", // 미래: 1일 남음
        "2025-01-01, 2025-01-01, 0", // 당일: 0일
        "2025-01-02, 2025-01-01, -1", // 과거: 1일 지남
        "2025-01-05, 2025-01-01, -4" // 과거: 4일 지남
    })
    void 기준_날짜와_소비기한의_차이를_일_단위로_계산한다(LocalDate now, LocalDate expirationDate, long expectedDays) {
        // given
        Food food = fixtureMonkey.giveMeBuilder(Food.class)
            .set("expirationDate", expirationDate.atStartOfDay())
            .sample();

        // when
        long daysLeft = food.calculateDaysLeft(now);

        // then
        assertThat(daysLeft).isEqualTo(expectedDays);
    }

    @Test
    void 소비기한의_시간이_다르더라도_날짜가_같으면_0일을_반환한다() {
        // given
        LocalDate now = LocalDate.of(2025, 1, 1);
        LocalDateTime expirationAtEndOfDay = LocalDateTime.of(2025, 1, 1, 23, 59, 59);
        Food food = food(fixtureMonkey, refrigerator, member, category)
            .set("expirationDate", expirationAtEndOfDay)
            .sample();

        // when
        long daysLeft = food.calculateDaysLeft(now);

        // then
        assertThat(daysLeft).isEqualTo(0);
    }

    @Test
    void 음식을_소비하면_재고가_차감된다() {
        // given
        Quantity initial = new Quantity(new BigDecimal("5.00"), Unit.PIECE);
        Food food = food(fixtureMonkey, refrigerator, member, category)
            .set("quantity", initial)
            .sample();

        Quantity amount = new Quantity(new BigDecimal("2.00"), Unit.PIECE);

        // when
        food.consume(amount);

        // then
        assertThat(food.getQuantity().amount())
            .isEqualTo(new BigDecimal("3.00"));
    }

    @Test
    void 음식을_추가하면_재고가_증가한다() {
        // given
        Quantity initial = new Quantity(new BigDecimal("1.0"), Unit.PIECE);
        Food food = food(fixtureMonkey, refrigerator, member, category)
            .set("quantity", initial)
            .sample();

        Quantity addAmount = new Quantity(new BigDecimal("2.0"), Unit.PIECE);

        // when
        food.add(addAmount);

        // then
        assertThat(food.getQuantity().amount())
            .isEqualByComparingTo(new BigDecimal("3.0"));
    }

    @Test
    void 재고가_0이면_소진_상태로_판단한다() {
        // given
        Quantity initial = new Quantity(new BigDecimal("1.0"), Unit.PIECE);
        Food food = food(fixtureMonkey, refrigerator, member, category)
            .set("quantity", initial)
            .sample();

        // when
        food.consume(initial);

        // then
        assertThat(food.isOutOfStock()).isTrue();
    }

    @Test
    void 재고가_남아있으면_소진_상태가_아니다() {
        // given
        Quantity initial = new Quantity(new BigDecimal("2.00"), Unit.PIECE);
        Food food = food(fixtureMonkey, refrigerator, member, category)
            .set("quantity", initial)
            .sample();

        Quantity amount = new Quantity(new BigDecimal("1.00"), Unit.PIECE);

        // when
        food.consume(amount);

        // then
        assertThat(food.isOutOfStock()).isFalse();
    }

}