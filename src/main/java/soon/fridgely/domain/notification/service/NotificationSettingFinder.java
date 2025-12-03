package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class NotificationSettingFinder {

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional(readOnly = true)
    public NotificationSetting findNotificationSetting(long memberId) {
        return notificationSettingRepository.findByMemberId(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

    @Transactional(readOnly = true)
    public List<NotificationSetting> findAllActiveByTime(LocalTime start, LocalTime end) {
        return notificationSettingRepository.findAllActiveByTime(start, end);
    }

}