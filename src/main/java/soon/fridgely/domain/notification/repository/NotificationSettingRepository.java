package soon.fridgely.domain.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soon.fridgely.domain.notification.entity.NotificationSetting;

import java.time.LocalTime;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByMemberId(long memberId);

    boolean existsByMemberId(long memberId);

    /**
     * 활성화된 알림 설정을 특정 시간대에 맞춰 모두 조회
     */
    @Query("""
            SELECT ns FROM NotificationSetting ns
            JOIN FETCH ns.member
            WHERE ns.enabled = true
            AND ns.alertSchedule.notificationTime BETWEEN :startTime AND :endTime
            AND ns.id < :cursorId
            ORDER BY ns.id DESC
        """)
    Slice<NotificationSetting> findAllActiveByTimeWithCursor(
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("cursorId") long cursorId,
        Pageable pageable
    );

    /**
     * 시간에 상관없이 활성화된 모든 알림 설정 조회
     */
    @Query("""
            SELECT ns FROM NotificationSetting ns
            JOIN FETCH ns.member
            WHERE ns.enabled = true
            AND ns.id < :cursorId
            ORDER BY ns.id DESC
        """)
    Slice<NotificationSetting> findAllActive(
        @Param("cursorId") long cursorId,
        Pageable pageable
    );

}