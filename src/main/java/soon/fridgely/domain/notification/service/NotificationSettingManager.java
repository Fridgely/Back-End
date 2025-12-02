package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@RequiredArgsConstructor
@Component
public class NotificationSettingManager {

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional
    public void createDefaultSetting(Member member) {
        if (notificationSettingRepository.existsByMemberId(member.getId())) {
            return;
        }

        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);
        notificationSettingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public NotificationSetting findNotificationSetting(long memberId) {
        return notificationSettingRepository.findByMemberId(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
    }

}