package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalTime;

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
    public Slice<NotificationSetting> findAllActiveByTime(LocalTime startTime, LocalTime endTime, long cursorId, Pageable pageable) {
        return notificationSettingRepository.findAllActiveByTimeWithCursor(startTime, endTime, cursorId, pageable);
    }

    @Transactional(readOnly = true)
    public Slice<NotificationSetting> findAllActive(long cursorId, Pageable pageable) {
        return notificationSettingRepository.findAllActive(cursorId, pageable);
    }

}