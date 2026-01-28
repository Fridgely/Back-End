package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import soon.fridgely.global.batch.BatchResult;
import soon.fridgely.domain.notification.batch.NotificationBatchExecutor;
import soon.fridgely.global.support.utils.TimeRangeUtils;

import java.time.LocalTime;

@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
@Service
public class NotificationService {

    private final NotificationBatchExecutor notificationBatchExecutor;
    private final NotificationProcessor notificationProcessor;

    @Scheduled(cron = "0 0 * * * *") // 매 정각마다 실행
    public BatchResult sendScheduledAlerts() {
        LocalTime now = LocalTime.now();

        return notificationBatchExecutor.executeForExpiration(
            TimeRangeUtils.startOfHour(now),
            TimeRangeUtils.endOfHour(now),
            setting -> {
                long memberId = setting.getMember().getId();
                notificationProcessor.processExpiration(memberId);
            }
        );
    }

    @Scheduled(cron = "0 30 10 * * *") // 매일 오전 10시 30분에 실행
    public BatchResult sendOutOfStockSummaries() {
        return notificationBatchExecutor.executeForStockSummary(
            setting -> {
                long memberId = setting.getMember().getId();
                notificationProcessor.processStockSummary(memberId);
            }
        );
    }

}