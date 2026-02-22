package soon.fridgely.global.support.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Slack 알림용 로그 마커
 */
public final class SlackMarkers {

    /**
     * 배치/스케줄러 작업 알림용
     */
    public static final Marker BATCH = MarkerFactory.getMarker("SLACK_BATCH");

    /**
     * 비즈니스 이벤트 알림용
     */
    public static final Marker BUSINESS = MarkerFactory.getMarker("SLACK_BUSINESS");

    /**
     * 시스템 상태 변경 알림용
     */
    public static final Marker SYSTEM = MarkerFactory.getMarker("SLACK_SYSTEM");

    /**
     * 성능 관련 알림용
     */
    public static final Marker PERFORMANCE = MarkerFactory.getMarker("SLACK_PERFORMANCE");

    private SlackMarkers() {
        throw new AssertionError("Cannot instantiate utility class");
    }

}