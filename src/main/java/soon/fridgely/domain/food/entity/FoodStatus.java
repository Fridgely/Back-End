package soon.fridgely.domain.food.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.requireNonNull;

public enum FoodStatus {

    BLACK,
    RED,
    YELLOW,
    GREEN;

    public static FoodStatus fromDaysLeft(LocalDate expirationDate, LocalDate now) {
        requireNonNull(expirationDate, "expirationDate는 필수입니다.");
        requireNonNull(now, "now는 필수입니다.");

        long daysLeft = ChronoUnit.DAYS.between(now, expirationDate);
        if (daysLeft <= 0) {
            return BLACK;
        }
        if (daysLeft <= 10) {
            return RED;
        }
        if (daysLeft <= 20) {
            return YELLOW;
        }
        return GREEN;
    }

}