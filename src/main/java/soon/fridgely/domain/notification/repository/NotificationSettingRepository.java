package soon.fridgely.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.notification.entity.NotificationSetting;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByMemberId(long memberId);

}