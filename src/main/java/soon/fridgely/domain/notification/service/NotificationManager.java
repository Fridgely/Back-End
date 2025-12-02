package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.repository.NotificationSettingRepository;

@RequiredArgsConstructor
@Component
public class NotificationManager {

    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional
    public void createDefaultSetting(Member member) {
        NotificationSetting setting = NotificationSetting.createDefaultSetting(member);
        notificationSettingRepository.save(setting);
    }

}