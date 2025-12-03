package soon.fridgely.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soon.fridgely.domain.notification.entity.NotificationSetting;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByMemberId(long memberId);

    boolean existsByMemberId(long memberId);

    /*
     * 특정 시간대에 알림이 활성화된 모든 NotificationSetting 조회
     */
    @Query("""
        SELECT ns FROM NotificationSetting ns
        JOIN FETCH ns.member
        WHERE ns.enabled = true
        AND ns.alertSchedule.notificationTime BETWEEN :startTime AND :endTime
    """)
    List<NotificationSetting> findAllActiveByTime(
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

}