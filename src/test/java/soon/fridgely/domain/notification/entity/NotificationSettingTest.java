package soon.fridgely.domain.notification.entity;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberFixture.member;

class NotificationSettingTest {

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    private Member member;

    @BeforeEach
    void setUp() {
        this.member = member(fixtureMonkey).sample();
    }

    @Test
    void 기본_알림_설정을_생성한다() {
        // when
        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);

        // then
        assertThat(setting)
            .extracting("member", "alertSchedule.notificationTime", "alertSchedule.daysBeforeExpiration", "enabled")
            .containsExactly(member, LocalTime.of(9, 0), 3, true);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 3, true",
        "9, 0, 5, false",
        "14, 30, 1, true",
        "23, 59, 30, false"
    })
    void 알림_설정을_수정한다(int hour, int minute, int days, boolean enabled) {
        // given
        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);
        var newSchedule = fixtureMonkey.giveMeBuilder(AlertSchedule.class)
            .set("notificationTime", LocalTime.of(hour, minute))
            .set("daysBeforeExpiration", days)
            .sample();

        // when
        setting.updateSettings(enabled, newSchedule);

        // then
        assertThat(setting)
            .extracting("enabled", "alertSchedule.notificationTime", "alertSchedule.daysBeforeExpiration")
            .containsExactly(enabled, LocalTime.of(hour, minute), days);
    }

}