package soon.fridgely.domain.food.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.requireNonNull;

public enum FoodStatus {

    BLACK(0),
    RED(10),
    YELLOW(20),
    GREEN(Integer.MAX_VALUE);

    /**
     * 해당 status가 적용되는 유통기한 잔여일 상한
     * BLACK=0, RED=10, YELLOW=20, GREEN=무제한
     */
    public final int daysThreshold;

    FoodStatus(int daysThreshold) {
        this.daysThreshold = daysThreshold;
    }

    /**
     * 해당 status 범위의 다음 날 (상한 + 1)
     * bulk update 쿼리의 날짜 범위 상한 계산에 사용
     * GREEN(Integer.MAX_VALUE)처럼 상한이 없는 status는 MAX_VALUE 그대로 반환
     */
    public int nextThresholdDay() {
        return daysThreshold == Integer.MAX_VALUE ? Integer.MAX_VALUE : daysThreshold + 1;
    }

    public static FoodStatus fromDaysLeft(LocalDate expirationDate, LocalDate now) {
        requireNonNull(expirationDate, "expirationDate는 필수입니다.");
        requireNonNull(now, "now는 필수입니다.");

        long daysLeft = ChronoUnit.DAYS.between(now, expirationDate);
        if (daysLeft <= BLACK.daysThreshold) {
            return BLACK;
        }
        if (daysLeft <= RED.daysThreshold) {
            return RED;
        }
        if (daysLeft <= YELLOW.daysThreshold) {
            return YELLOW;
        }
        return GREEN;
    }

    public static FoodStatus fromDaysLeft(LocalDateTime expirationDate, LocalDate now) {
        requireNonNull(expirationDate, "expirationDate는 필수입니다.");
        requireNonNull(now, "now는 필수입니다.");
        return fromDaysLeft(expirationDate.toLocalDate(), now);
    }

}