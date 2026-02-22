package soon.fridgely.global.support.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.List;

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

    private boolean hasSlackMarker(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        if (markers == null || markers.isEmpty()) {
            return false;
        }

        return markers.stream()
            .filter(marker -> marker != null && marker.getName() != null)
            .anyMatch(marker -> marker.getName().startsWith("SLACK_"));
    }

}