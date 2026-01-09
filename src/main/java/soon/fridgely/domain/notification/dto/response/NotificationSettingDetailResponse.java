package soon.fridgely.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import soon.fridgely.domain.notification.entity.NotificationSetting;

import java.time.LocalTime;

@Schema(description = "알림 설정 상세 응답")
public record NotificationSettingDetailResponse(

    @Schema(description = "알림 시간", example = "09:00:00")
    LocalTime notificationTime,

    @Schema(description = "유통기한 기준일", example = "3")
    int daysBeforeExpiration,

    @Schema(description = "알림 활성화 여부", example = "true")
    boolean enabled

) {

    public static NotificationSettingDetailResponse from(NotificationSetting setting) {
        return new NotificationSettingDetailResponse(
            setting.getAlertSchedule().notificationTime(),
            setting.getAlertSchedule().daysBeforeExpiration(),
            setting.isEnabled()
        );
    }

}