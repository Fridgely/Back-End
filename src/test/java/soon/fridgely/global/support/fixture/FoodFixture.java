package soon.fridgely.global.support.fixture;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import net.jqwik.api.Arbitraries;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.entity.*;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FixtureMonkey를 사용하여 Food 엔티티의 테스트 데이터를 생성하는 유틸리티 클래스
 */
public final class FoodFixture {

    private FoodFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ArbitraryBuilder<Food> food(
        FixtureMonkey fixtureMonkey,
        Refrigerator refrigerator,
        Member member,
        Category category
    ) {
        return fixtureMonkey.giveMeBuilder(Food.class)
            .set("name", Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50))
            .set("imageURL", Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(512))
            .set("description", Arbitraries.strings().alpha().ofMaxLength(255))
            .set("storageType", Arbitraries.of(StorageType.class))
            .set("foodStatus", Arbitraries.of(FoodStatus.class))
            .set("expirationDate", Arbitraries.integers().between(1, 365)
                .map(days -> LocalDateTime.now().plusDays(days)))
            .set("quantity", Arbitraries.integers().between(1, 100)
                .map(amount -> Quantity.register(BigDecimal.valueOf(amount), Unit.PIECE)))
            .set("refrigerator", refrigerator)
            .set("member", member)
            .set("category", category)
            .set("status", EntityStatus.ACTIVE)
            .setNull("id");
    }

    public static ArbitraryBuilder<Food> food(
        FixtureMonkey fixtureMonkey,
        Refrigerator refrigerator,
        Member member,
        Category category,
        LocalDateTime expirationDate,
        FoodStatus foodStatus
    ) {
        return food(fixtureMonkey, refrigerator, member, category)
            .set("expirationDate", expirationDate)
            .set("foodStatus", foodStatus);
    }

    /**
     * FoodStatus 범위의 만료일 반환
     * - BLACK : 어제 (만료)
     * - RED    : RED 범위 중간값
     * - YELLOW : YELLOW 범위 중간값
     * - GREEN  : YELLOW 상한 이후 10일
     */
    public static LocalDateTime expirationDayFor(FoodStatus status, LocalDate today) {
        return switch (status) {
            case BLACK -> today.minusDays(1).atStartOfDay();
            case RED -> today.plusDays(FoodStatus.RED.daysThreshold / 2).atStartOfDay();
            case YELLOW -> today.plusDays((FoodStatus.RED.daysThreshold + FoodStatus.YELLOW.daysThreshold) / 2).atStartOfDay();
            case GREEN -> today.plusDays(FoodStatus.YELLOW.nextThresholdDay() + 10).atStartOfDay();
        };
    }

}