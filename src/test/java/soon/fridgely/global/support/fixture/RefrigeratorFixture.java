package soon.fridgely.global.support.fixture;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import net.jqwik.api.Arbitraries;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

/**
 * FixtureMonkey를 사용하여 Refrigerator 엔티티의 테스트 데이터를 생성하는 유틸리티 클래스
 */
public final class RefrigeratorFixture {

    private RefrigeratorFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ArbitraryBuilder<Refrigerator> refrigerator(FixtureMonkey fixtureMonkey) {
        return fixtureMonkey.giveMeBuilder(Refrigerator.class)
            .set("name", Arbitraries.strings().ofMaxLength(50))
            .set("status", EntityStatus.ACTIVE) // DELETED 상태가 필요한 경우 사용시 지정
            .setNull("id"); // JPA 자동 생성 필드 null 처리
    }

}