package soon.fridgely.domain.notification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import soon.fridgely.domain.notification.entity.AlertSchedule;

import java.time.LocalTime;

public record NotificationSettingUpdateRequest(

    @NotNull(message = "알림 시간은 필수입니다.")
    LocalTime notificationTime,

    @Positive(message = "알림 기준일은 1일 이상이어야 합니다.")
    @Max(value = 30, message = "알림 기준일은 최대 30일까지 설정 가능합니다.")
    int daysBeforeExpiration,

    @NotNull(message = "알림 활성화 여부는 필수입니다.")
    Boolean enabled

) {

    public AlertSchedule toAlertSchedule() {
        return AlertSchedule.of(notificationTime, daysBeforeExpiration);
    }

}