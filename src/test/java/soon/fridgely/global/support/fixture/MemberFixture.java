package soon.fridgely.global.support.fixture;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import net.jqwik.api.Arbitraries;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;

/**
 * FixtureMonkey를 사용하여 Member 엔티티의 테스트 데이터를 생성하는 유틸리티 클래스
 */
public final class MemberFixture {

    private MemberFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ArbitraryBuilder<Member> member(FixtureMonkey fixtureMonkey) {
        return fixtureMonkey.giveMeBuilder(Member.class)
            .set("nickname", Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20))
            .set("loginId", Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100))
            .set("password", Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100))
            .set("role", Arbitraries.of(MemberRole.class))
            .set("status", EntityStatus.ACTIVE) // DELETED 상태가 필요한 경우 사용시 지정
            .setNull("id") // JPA 자동 생성 필드 null 처리
            .setNull("refreshToken"); // 초기 상태에서 refreshToken은 null
    }

}