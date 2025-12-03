package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.domain.notification.batch.NotificationBatchExecutor;
import soon.fridgely.global.support.utils.TimeRangeUtils;

import java.time.LocalTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final NotificationBatchExecutor notificationBatchExecutor;
    private final NotificationProcessor notificationProcessor;

    public void sendScheduledAlerts() {
        LocalTime now = LocalTime.now();
        LocalTime start = TimeRangeUtils.startOfHour(now);
        LocalTime end = TimeRangeUtils.endOfHour(now);

        log.info("[Notification Batch] {}시 알림 발송 시작", now.getHour());
        BatchResult result = notificationBatchExecutor.execute(start, end, notificationProcessor::process);
        log.info("[Notification Batch] {}시 알림 발송 종료 - {}", now.getHour(), result);
    }

}