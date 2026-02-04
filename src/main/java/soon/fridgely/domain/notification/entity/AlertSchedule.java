package soon.fridgely.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalTime;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertSchedule {

    private static final LocalTime DEFAULT_NOTIFICATION_TIME = LocalTime.of(9, 0);
    private static final int DEFAULT_DAYS_BEFORE_EXPIRATION = 3;
    private static final int MAX_DAYS = 30;

    @Column(nullable = false)
    private LocalTime notificationTime;

    @Column(nullable = false)
    private int daysBeforeExpiration;

    private AlertSchedule(LocalTime notificationTime, int daysBeforeExpiration) {
        validate(notificationTime, daysBeforeExpiration);
        this.notificationTime = notificationTime;
        this.daysBeforeExpiration = daysBeforeExpiration;
    }

    public static AlertSchedule of(LocalTime time, int days) {
        return new AlertSchedule(time, days);
    }

    public static AlertSchedule createDefault() {
        return new AlertSchedule(DEFAULT_NOTIFICATION_TIME, DEFAULT_DAYS_BEFORE_EXPIRATION);
    }

    private void validate(LocalTime time, int days) {
        if (time == null) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "알림 시간은 null일 수 없습니다.");
        }
        if (days <= 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "만료일 날짜는 0보다 커야 합니다.");
        }
        if (days > MAX_DAYS) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "알림 기준일은 최대 " + MAX_DAYS + "일까지 설정 가능합니다.");
        }
    }

}