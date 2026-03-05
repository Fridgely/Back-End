package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.notification.dto.request.NotificationSettingUpdateRequest;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.domain.notification.entity.NotificationSetting;

@RequiredArgsConstructor
@Service
public class NotificationSettingService {

    private final NotificationSettingManager notificationSettingManager;
    private final NotificationSettingFinder notificationSettingFinder;

    public NotificationSettingDetailResponse findNotificationSetting(long memberId) {
        NotificationSetting setting = notificationSettingFinder.findNotificationSetting(memberId);
        return NotificationSettingDetailResponse.from(setting);
    }

    public void updateNotificationSetting(long memberId, NotificationSettingUpdateRequest request) {
        notificationSettingManager.update(memberId, request.toAlertSchedule(), request.enabled());
    }

}