package soon.fridgely.global.support.fixture;

import com.navercorp.fixturemonkey.ArbitraryBuilder;
import com.navercorp.fixturemonkey.FixtureMonkey;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.notification.entity.NotificationSetting;

/**
 * FixtureMonkey를 사용하여 NotificationSetting 엔티티의 테스트 데이터를 생성하는 유틸리티 클래스
 */
public final class NotificationSettingFixture {

    private NotificationSettingFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ArbitraryBuilder<NotificationSetting> notificationSetting(
        FixtureMonkey fixtureMonkey,
        Member member
    ) {
        return fixtureMonkey.giveMeBuilder(NotificationSetting.class)
            .set("member", member)
            .set("alertSchedule", AlertSchedule.createDefault())
            .set("enabled", true)
            .set("status", EntityStatus.ACTIVE) // DELETED 상태가 필요한 경우 사용시 지정
            .setNull("id"); // JPA 자동 생성 필드 null 처리
    }

}