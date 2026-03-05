package soon.fridgely.global.support.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeRangeUtils {

    /**
     * 해당 시간의 정각(00분 00초 00나노) 반환
     */
    public static LocalTime startOfHour(LocalTime time) {
        return time.withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 해당 시간의 마지막(59분 59초 999...나노) 반환
     */
    public static LocalTime endOfHour(LocalTime time) {
        return time.withMinute(59).withSecond(59).withNano(999_999_999);
    }

    /**
     * 해당 날짜의 시작(00:00:00) 반환
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * 해당 날짜의 마지막(23:59:59.999...) 반환
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

}