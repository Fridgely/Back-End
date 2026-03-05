package soon.fridgely.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.member.entity.Member;

import static java.util.Objects.requireNonNull;

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

    @Embedded
    private AlertSchedule alertSchedule;

    @Column(nullable = false)
    private boolean enabled;

    /**
     * 회원가입 시 기본 설정 생성
     * - 시간: 오전 9시
     * - 기준: 3일 전
     * - 알림: 켜짐
     */
    public static NotificationSetting createDefaultSetting(Member member) {
        return NotificationSetting.builder()
            .member(member)
            .alertSchedule(AlertSchedule.createDefault())
            .enabled(true)
            .build();
    }

    public void updateSettings(boolean enabled, AlertSchedule alertSchedule) {
        this.alertSchedule = requireNonNull(alertSchedule, "스케줄은 필수입니다.");
        this.enabled = enabled;
    }

}