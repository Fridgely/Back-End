package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.global.support.CursorPageRequest;
import soon.fridgely.global.support.utils.TimeRangeUtils;

import java.time.LocalTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private static final int BATCH_SIZE = 100;

    private final NotificationSettingFinder notificationSettingFinder;
    private final NotificationProcessor notificationProcessor;

    /*
     * 스케줄러에 의해 매시 정각 호출
     * TODO: 차후 재시도 로직 및 비동기 처리 고려
     */
    public void sendScheduledAlerts() {
        LocalTime now = LocalTime.now();
        LocalTime start = TimeRangeUtils.startOfHour(now);
        LocalTime end = TimeRangeUtils.endOfHour(now);
        log.info("[Notification Batch] {}시 알림 발송 시작", now.getHour());

        Long cursorId = null;
        int totalRequest = 0;

        while (true) {
            CursorPageRequest cursorRequest = new CursorPageRequest(cursorId, BATCH_SIZE);
            Slice<NotificationSetting> slice = notificationSettingFinder.findAllActiveByTime(start, end, cursorRequest.getCursorId(), cursorRequest.toPageable());
            if (slice.isEmpty()) {
                break;
            }

            slice.forEach(notificationProcessor::process);
            totalRequest += slice.getNumberOfElements();

            if (!slice.hasNext()) {
                break;
            }

            cursorId = slice.getContent().get(slice.getNumberOfElements() - 1).getId();
        }

        log.info("[Notification Batch] {}시 알림 발송 종료 (총 {}건)", now.getHour(), totalRequest);
    }

}