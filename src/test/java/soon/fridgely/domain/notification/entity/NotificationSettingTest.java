package soon.fridgely.domain.notification.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationSettingTest {

    @Test
    void 기본_알림_설정을_생성한다() {
        // given
        Member member = createMember();

        // when
        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);

        // then
        assertThat(setting)
            .extracting("member", "notificationTime", "daysBeforeExpiration", "enabled")
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
        LocalTime time = LocalTime.of(hour, minute);

        // when
        setting.updateSettings(time, days, enabled);

        // then
        assertThat(setting)
            .extracting("notificationTime", "daysBeforeExpiration", "enabled")
            .containsExactly(time, days, enabled);
    }

    @Test
    void 알림_시간이_null이면_예외가_발생한다() {
        // given
        Member member = createMember();
        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);

        // expected
        assertThatThrownBy(() -> setting.updateSettings(null, 3, true))
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("알림 시간은 null일 수 없습니다.")
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    void 만료일이_0_이하이면_예외가_발생한다(int days) {
        // given
        Member member = createMember();
        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);

        // expected
        assertThatThrownBy(() -> setting.updateSettings(LocalTime.of(9, 0), days, true))
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("만료일 날짜는 0보다 커야 합니다.")
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 50, 100})
    void 만료일이_최댓값을_초과하면_예외가_발생한다(int days) {
        // given
        Member member = createMember();
        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);

        // expected
        assertThatThrownBy(() -> setting.updateSettings(LocalTime.of(9, 0), days, true))
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("알림 기준일은 최대 30일까지 설정 가능합니다.")
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
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