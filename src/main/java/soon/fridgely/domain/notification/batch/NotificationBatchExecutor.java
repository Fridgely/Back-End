package soon.fridgely.domain.notification.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.service.NotificationSettingFinder;
import soon.fridgely.global.support.CursorPageRequest;

import java.time.LocalTime;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationBatchExecutor {

    private static final int BATCH_SIZE = 100;
    private final NotificationSettingFinder notificationSettingFinder;

    /**
     * 유통기한 알림용으로 특정 시간대 타겟 실행
     */
    public BatchResult executeForExpiration(
        LocalTime startTime,
        LocalTime endTime,
        Consumer<NotificationSetting> task
    ) {
        return executeInternal(
            cursorRequest -> notificationSettingFinder.findAllActiveByTime(
                startTime,
                endTime,
                cursorRequest.getCursorId(),
                cursorRequest.toPageable()
            ),
            task
        );
    }

    /**
     * 재고 소진 알림용으로 시간에 무관한 타겟 실행
     */
    public BatchResult executeForStockSummary(Consumer<NotificationSetting> task) {
        return executeInternal(
            cursorRequest -> notificationSettingFinder.findAllActive(
                cursorRequest.getCursorId(),
                cursorRequest.toPageable()
            ),
            task
        );
    }

    /**
     * 커서기반 페이징 루프 처리
     * fetcher: 데이터를 어떻게 가져올지 정의한 함수
     */
    private BatchResult executeInternal(
        Function<CursorPageRequest, Slice<NotificationSetting>> fetcher,
        Consumer<NotificationSetting> task
    ) {
        StopWatch stopWatch = new StopWatch("Notification Batch Executor");
        stopWatch.start();

        Long cursorId = null;
        int totalRequest = 0;

        while (true) {
            CursorPageRequest cursorRequest = new CursorPageRequest(cursorId, BATCH_SIZE);
            Slice<NotificationSetting> slice = fetcher.apply(cursorRequest);

            if (slice.isEmpty()) {
                break;
            }

            slice.forEach(task);
            int fetched = slice.getNumberOfElements();
            totalRequest += fetched;

            if (!slice.hasNext()) {
                break;
            }

            cursorId = slice.getContent().get(fetched - 1).getId();
        }

        stopWatch.stop();
        return BatchResult.of(totalRequest, stopWatch.getTotalTimeMillis());
    }

}