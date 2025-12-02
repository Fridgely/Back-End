package soon.fridgely.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.member.entity.Member;

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
        this.notificationTime = notificationTime;
        this.daysBeforeExpiration = daysBeforeExpiration;
        this.enabled = enabled;
    }

}
