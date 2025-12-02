package soon.fridgely.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalTime;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "notification_settings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_settings_member_id", columnNames = "member_id")
    }
)
@Entity
public class NotificationSetting extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalTime notificationTime;

    @Column(nullable = false)
    private int daysBeforeExpiration;

    @Column(nullable = false)
    private boolean enabled;

    private static final LocalTime DEFAULT_NOTIFICATION_TIME = LocalTime.of(9, 0);
    private static final int DEFAULT_DAYS_BEFORE_EXPIRATION = 3;
    private static final int MAX_DAYS = 30;

    /**
     * 회원가입 시 기본 설정 생성
     * - 시간: 오전 9시
     * - 기준: 3일 전
     * - 알림: 켜짐
     */
    public static NotificationSetting createDefaultSetting(Member member) {
        return NotificationSetting.builder()
            .member(member)
            .notificationTime(DEFAULT_NOTIFICATION_TIME)
            .daysBeforeExpiration(DEFAULT_DAYS_BEFORE_EXPIRATION)
            .enabled(true)
            .build();
    }

    public void updateSettings(LocalTime notificationTime, int daysBeforeExpiration, boolean enabled) {
        validateNotificationTime(notificationTime);
        validateDaysBeforeExpiration(daysBeforeExpiration);

        this.notificationTime = notificationTime;
        this.daysBeforeExpiration = daysBeforeExpiration;
        this.enabled = enabled;
    }

    private void validateNotificationTime(LocalTime notificationTime) {
        if (notificationTime == null) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "알림 시간은 null일 수 없습니다.");
        }
    }

    private void validateDaysBeforeExpiration(int daysBeforeExpiration) {
        if (daysBeforeExpiration <= 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "만료일 날짜는 0보다 커야 합니다.");
        }
        if (daysBeforeExpiration > MAX_DAYS) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "알림 기준일은 최대 " + MAX_DAYS + "일까지 설정 가능합니다.");
        }
    }

}