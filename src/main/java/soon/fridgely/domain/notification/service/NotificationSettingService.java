package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.dto.request.NotificationSettingUpdateRequest;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalTime;

@RequiredArgsConstructor
@Service
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationProcessor notificationProcessor;

    @Transactional(readOnly = true)
    public NotificationSettingDetailResponse findNotificationSetting(long memberId) {
        NotificationSetting setting = findByMemberId(memberId);
        return NotificationSettingDetailResponse.from(setting);
    }

    @Transactional
    public void updateNotificationSetting(long memberId, NotificationSettingUpdateRequest request) {
        NotificationSetting setting = findByMemberId(memberId);
        setting.updateSettings(request.enabled(), request.toAlertSchedule());
    }

    public void triggerExpirationTest(long memberId) {
        notificationProcessor.processExpiration(memberId);
    }

    @Transactional
    public void createDefaultSetting(Member member) {
        if (notificationSettingRepository.existsByMemberId(member.getId())) {
            return;
        }
        notificationSettingRepository.save(NotificationSetting.createDefaultSetting(member));
    }

    @Transactional(readOnly = true)
    public NotificationSetting findByMemberId(long memberId) {
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
