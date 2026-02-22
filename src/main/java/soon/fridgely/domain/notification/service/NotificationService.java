package soon.fridgely.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.notification.batch.NotificationBatchExecutor;
import soon.fridgely.global.batch.BatchResult;
import soon.fridgely.global.support.logging.SlackMarkers;
import soon.fridgely.global.support.utils.TimeRangeUtils;

import java.time.LocalTime;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
@Service
public class NotificationService {

    private final NotificationBatchExecutor notificationBatchExecutor;
    private final NotificationProcessor notificationProcessor;

    @Scheduled(cron = "0 0 * * * *") // 매 정각마다 실행
    public BatchResult sendScheduledAlerts() {
        LocalTime now = LocalTime.now();

        BatchResult result = notificationBatchExecutor.executeForExpiration(
            TimeRangeUtils.startOfHour(now),
            TimeRangeUtils.endOfHour(now),
            setting -> {
                long memberId = setting.getMember().getId();
                notificationProcessor.processExpiration(memberId);
            }
        );

        log.info(SlackMarkers.BATCH,
            "[유통기한 알림 배치 완료] 시간대: {}, 처리건수: {}건, 소요시간: {}ms",
            now.getHour() + "시",
            result.submittedCount(),
            result.durationMillis()
        );

        return result;
    }

    @Scheduled(cron = "0 30 10 * * *") // 매일 오전 10시 30분에 실행
    public BatchResult sendOutOfStockSummaries() {
        BatchResult result = notificationBatchExecutor.executeForStockSummary(
            setting -> {
                long memberId = setting.getMember().getId();
                notificationProcessor.processStockSummary(memberId);
            }
        );

        log.info(SlackMarkers.BATCH,
            "[재고 소진 알림 배치 완료] 처리: {}건, 소요: {}ms",
            result.submittedCount(),
            result.durationMillis()
        );

        return result;
    }

}