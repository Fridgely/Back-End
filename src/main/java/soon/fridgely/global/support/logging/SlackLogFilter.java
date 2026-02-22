package soon.fridgely.global.support.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * Slack 전송용 로그 필터
 */
public class SlackLogFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(Level.ERROR) || hasSlackMarker(event)) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;
    }

    @SuppressWarnings("deprecation") // Logback의 표준 API
    private boolean hasSlackMarker(ILoggingEvent event) {
        Marker marker = event.getMarker();
        return marker != null && marker.getName().startsWith("SLACK_");
    }
}