package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "FoodStatusScheduler.updateFoodStatuses", lockAtLeastFor = "PT10S", lockAtMostFor = "PT5M")
    public BatchResult updateFoodStatuses() {
        LocalDate today = LocalDate.now();

        log.debug("[FoodStatusScheduler] 시작. (Today={})", today);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int totalUpdated = foodStatusProcessor.bulkUpdate(today);
        stopWatch.stop();

        BatchResult result = BatchResult.of(totalUpdated, stopWatch.getTotalTimeMillis());

        log.info(SlackMarkers.BATCH,
            "[FoodStatusScheduler] 배치 완료. (Today={}, Count={}, DurationMs={})",
            today, result.submittedCount(), result.durationMillis()
        );

        return result;
    }

}