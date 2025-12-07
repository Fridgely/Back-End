package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.domain.notification.batch.NotificationBatchExecutor;
import soon.fridgely.global.support.utils.TimeRangeUtils;

import java.time.LocalTime;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final NotificationBatchExecutor notificationBatchExecutor;
    private final NotificationProcessor notificationProcessor;

    @Scheduled(cron = "0 0 * * * *") // 매 정각마다 실행
    public BatchResult sendScheduledAlerts() {
        LocalTime now = LocalTime.now();

        return notificationBatchExecutor.execute(
            TimeRangeUtils.startOfHour(now),
            TimeRangeUtils.endOfHour(now),
            notificationProcessor::process
        );
    }

}