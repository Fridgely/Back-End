package soon.fridgely.global.support.fixture;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.global.support.FixtureMonkeyFactory;

/**
 * FixtureMonkey를 사용하여 MemberRefrigerator 엔티티의 테스트 데이터를 생성하는 유틸리티 클래스
 */
public final class MemberRefrigeratorFixture {

    private static final FixtureMonkey FIXTURE_MONKEY = FixtureMonkeyFactory.get();

    private MemberRefrigeratorFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ArbitraryBuilder<MemberRefrigerator> memberRefrigerator(
        Refrigerator refrigerator,
        Member member
    ) {
        return FIXTURE_MONKEY.giveMeBuilder(MemberRefrigerator.class)
            .set("refrigerator", refrigerator)
            .set("member", member)
            .set("status", EntityStatus.ACTIVE) // DELETED 상태가 필요한 경우 사용시 지정
            .setNull("id"); // JPA 자동 생성 필드 null 처리
    }

}