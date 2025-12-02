package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.domain.notification.entity.NotificationSetting;

@RequiredArgsConstructor
@Service
public class NotificationSettingService {

    private final NotificationSettingManager notificationSettingManager;

    public NotificationSettingDetailResponse findNotificationSetting(long memberId) {
        NotificationSetting setting = notificationSettingManager.findNotificationSetting(memberId);
        return NotificationSettingDetailResponse.from(setting);
    }

}