package soon.fridgely.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import soon.fridgely.domain.notification.entity.AlertSchedule;

import java.time.LocalTime;

@Schema(description = "알림 설정 수정 요청")
public record NotificationSettingUpdateRequest(

    @Schema(description = "알림 시간", example = "09:00:00")
    @NotNull(message = "알림 시간은 필수입니다.")
    LocalTime notificationTime,

    @Schema(description = "유통기한 몇 일 전에 알림을 받을지 (1~30)", example = "3")
    @Positive(message = "알림 기준일은 1일 이상이어야 합니다.")
    @Max(value = 30, message = "알림 기준일은 최대 30일까지 설정 가능합니다.")
    int daysBeforeExpiration,

    @Schema(description = "알림 활성화 여부", example = "true")
    @NotNull(message = "알림 활성화 여부는 필수입니다.")
    Boolean enabled

) {

    public AlertSchedule toAlertSchedule() {
        return AlertSchedule.of(notificationTime, daysBeforeExpiration);
    }

}