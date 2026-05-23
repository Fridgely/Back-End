package soon.fridgely.global.support.fixture;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import net.jqwik.api.Arbitraries;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.global.support.FixtureMonkeyFactory;

/**
 * FixtureMonkey를 사용하여 Category 엔티티의 테스트 데이터를 생성하는 유틸리티 클래스
 */
public final class CategoryFixture {

    private static final FixtureMonkey FIXTURE_MONKEY = FixtureMonkeyFactory.get();

    private CategoryFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ArbitraryBuilder<Category> category(
        Refrigerator refrigerator,
        Member member
    ) {
        return FIXTURE_MONKEY.giveMeBuilder(Category.class)
            .set("name", Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100))
            .set("type", Arbitraries.of(CategoryType.class))
            .set("member", member)
            .set("refrigerator", refrigerator)
            .set("status", EntityStatus.ACTIVE) // DELETED 상태가 필요한 경우 사용시 지정
            .setNull("id"); // JPA 자동 생성 필드 null 처리
    }

}