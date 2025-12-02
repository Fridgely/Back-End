package soon.fridgely.domain.notification.dto.response;

import soon.fridgely.domain.notification.entity.NotificationSetting;

import java.time.LocalTime;

public record NotificationSettingDetailResponse(
    LocalTime notificationTime,
    int daysBeforeExpiration,
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