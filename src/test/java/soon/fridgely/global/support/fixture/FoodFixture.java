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

}