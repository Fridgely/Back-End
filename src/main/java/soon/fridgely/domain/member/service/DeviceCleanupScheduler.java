package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.member.batch.DeviceCleanupBatchExecutor;
import soon.fridgely.domain.member.batch.DeviceIdBuffer;
import soon.fridgely.global.batch.BatchResult;
import soon.fridgely.global.support.logging.SlackMarkers;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
@Service
public class DeviceCleanupScheduler {

    // 토큰 삭제 기준: 90일(3개월)
    private static final int INACTIVE_DAYS_THRESHOLD = 90;

    private final DeviceCleanupBatchExecutor deviceCleanupBatchExecutor;
    private final DeviceCleanupProcessor deviceCleanupProcessor;

    @Scheduled(cron = "0 30 2 * * *")
    @SchedulerLock(name = "DeviceCleanupScheduler.cleanupInactiveDevices", lockAtLeastFor = "PT10S", lockAtMostFor = "PT5M")
    public BatchResult cleanupInactiveDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(INACTIVE_DAYS_THRESHOLD);

        log.debug("[DeviceCleanup] 시작 (Threshold={}, InactiveDays={})", threshold, INACTIVE_DAYS_THRESHOLD);

        DeviceIdBuffer buffer = new DeviceIdBuffer();

        BatchResult result = deviceCleanupBatchExecutor.executeCleanup(
            threshold, device -> buffer.add(device.getId(), deviceCleanupProcessor::bulkDelete)
        );

        buffer.flush(deviceCleanupProcessor::bulkDelete);

        log.info(SlackMarkers.BATCH,
            "[DeviceCleanup 배치 완료] 처리: {}건, 소요: {}ms",
            result.submittedCount(),
            result.durationMillis()
        );

        return result;
    }

}