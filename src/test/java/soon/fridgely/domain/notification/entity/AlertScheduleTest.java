package soon.fridgely.domain.notification.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertScheduleTest {

    @Test
    void 기본_알림_스케줄을_생성한다() {
        // given
        AlertSchedule schedule = AlertSchedule.createDefault();

        // expected
        assertThat(schedule.getNotificationTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.getDaysBeforeExpiration()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1",
        "9, 0, 5",
        "14, 30, 15",
        "23, 59, 30"
    })
    void 알림_스케줄을_생성한다(int hour, int minute, int days) {
        // given
        LocalTime time = LocalTime.of(hour, minute);

        // when
        AlertSchedule schedule = AlertSchedule.of(time, days);

        // then
        assertThat(schedule.getNotificationTime()).isEqualTo(time);
        assertThat(schedule.getDaysBeforeExpiration()).isEqualTo(days);
    }

    @Test
    void 알림_시간이_null이면_예외가_발생한다() {
        // expected
        assertThatThrownBy(() -> AlertSchedule.of(null, 3))
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("알림 시간은 null일 수 없습니다.")
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    void 만료일이_0_이하이면_예외가_발생한다(int days) {
        // expected
        assertThatThrownBy(() -> AlertSchedule.of(LocalTime.of(9, 0), days))
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("만료일 날짜는 0보다 커야 합니다.")
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 50, 100})
    void 만료일이_최댓값을_초과하면_예외가_발생한다(int days) {
        // expected
        assertThatThrownBy(() -> AlertSchedule.of(LocalTime.of(9, 0), days))
            .isInstanceOf(CoreException.class)
            .hasMessageContaining("알림 기준일은 최대 30일까지 설정 가능합니다.")
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
    }

}