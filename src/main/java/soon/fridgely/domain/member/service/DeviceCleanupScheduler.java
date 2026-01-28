package soon.fridgely.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import soon.fridgely.domain.member.batch.DeviceCleanupBatchExecutor;
import soon.fridgely.global.batch.BatchResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
@Service
public class DeviceCleanupScheduler {

    // 토큰 삭제 기준: 90일(3개월)
    private static final int INACTIVE_DAYS_THRESHOLD = 90;

    private final DeviceCleanupBatchExecutor deviceCleanupBatchExecutor;
    private final DeviceCleanupProcessor deviceCleanupProcessor;

    /**
     * 매일 새벽 2시 30분 장기 미사용 FCM 토큰 정리
     */
    @Scheduled(cron = "0 30 2 * * *")
    public BatchResult cleanupInactiveDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(INACTIVE_DAYS_THRESHOLD);

        log.info("[DeviceCleanup] 장기 미사용 디바이스 토큰 정리 시작. (Threshold={}, InactiveDays={})", threshold, INACTIVE_DAYS_THRESHOLD);

        // 100개씩 조회하면서 1000개 단위로 모아서 벌크 삭제
        List<Long> buffer = new ArrayList<>(DeviceCleanupProcessor.CHUNK_SIZE);

        BatchResult result = deviceCleanupBatchExecutor.executeCleanup(
            threshold, device -> {
                buffer.add(device.getId());
                if (buffer.size() >= DeviceCleanupProcessor.CHUNK_SIZE) {
                    deviceCleanupProcessor.bulkDelete(new ArrayList<>(buffer));
                    buffer.clear();
                }
            }
        );

        // 버퍼에 남은 데이터 최종 처리
        if (!buffer.isEmpty()) {
            deviceCleanupProcessor.bulkDelete(buffer);
        }

        log.info("[DeviceCleanup] 장기 미사용 디바이스 토큰 정리 완료. (Result={})", result);
        return result;
    }

}