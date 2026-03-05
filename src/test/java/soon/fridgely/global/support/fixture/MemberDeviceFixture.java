package soon.fridgely.global.support.fixture;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import net.jqwik.api.Arbitraries;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberDevice;

import java.time.LocalDateTime;

/**
 * FixtureMonkey를 사용하여 MemberDevice 엔티티의 테스트 데이터를 생성하는 유틸리티 클래스
 */
public final class MemberDeviceFixture {

    private MemberDeviceFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ArbitraryBuilder<MemberDevice> memberDevice(FixtureMonkey fixtureMonkey, Member member) {
        return fixtureMonkey.giveMeBuilder(MemberDevice.class)
            .set("member", member)
            .set("token", Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(255))
            .set("lastUsedAt", LocalDateTime.of(2024, 1, 1, 0, 0))
            .set("status", EntityStatus.ACTIVE)
            .setNull("id");
    }

}