package soon.fridgely.domain.notification.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSettingTest {

    @Test
    void 기본_알림_설정을_생성한다() {
        // given
        Member member = createMember();

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
        Member member = createMember();
        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);
        AlertSchedule newSchedule = AlertSchedule.of(LocalTime.of(hour, minute), days);

        // when
        setting.updateSettings(enabled, newSchedule);

        // then
        assertThat(setting)
            .extracting("enabled", "alertSchedule.notificationTime", "alertSchedule.daysBeforeExpiration")
            .containsExactly(enabled, LocalTime.of(hour, minute), days);
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

}