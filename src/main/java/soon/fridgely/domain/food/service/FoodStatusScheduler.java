package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import soon.fridgely.global.batch.BatchResult;
import soon.fridgely.global.support.logging.SlackMarkers;

import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
@Service
public class FoodStatusScheduler {

    private final FoodStatusProcessor foodStatusProcessor;

    /**
     * 매일 자정 유통기한 기반 FoodStatus 일괄 갱신
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public BatchResult updateFoodStatuses() {
        LocalDate today = LocalDate.now();

        log.debug("[FoodStatusScheduler] 시작 (기준일={})", today);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int totalUpdated = foodStatusProcessor.bulkUpdate(today);
        stopWatch.stop();

        BatchResult result = BatchResult.of(totalUpdated, stopWatch.getTotalTimeMillis());

        log.info(SlackMarkers.BATCH,
            "[FoodStatusScheduler 배치 완료] 기준일: {}, 총 변경: {}건, 소요: {}ms",
            today, result.submittedCount(), result.durationMillis()
        );

        return result;
    }

}