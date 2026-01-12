package soon.fridgely.domain.notification.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.service.NotificationSettingFinder;
import soon.fridgely.global.batch.AbstractBatchExecutor;

import java.time.LocalTime;
import java.util.function.Consumer;

/**
 * 알림 배치 처리를 위한 실행기
 *
 * <p>AbstractBatchExecutor를 상속받아 커서 기반 페이징 로직을 재사용
 * DeviceCleanupBatchExecutor와 동일한 구조로 일관성을 유지합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationBatchExecutor extends AbstractBatchExecutor<NotificationSetting> {

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
        return execute(
            cursorRequest -> notificationSettingFinder.findAllActiveByTime(
                startTime,
                endTime,
                cursorRequest.getCursorId(),
                cursorRequest.toPageable()
            ),
            task,
            "Expiration Notification Batch",
            BATCH_SIZE
        );
    }

    /**
     * 재고 소진 알림용으로 시간에 무관한 타겟 실행
     */
    public BatchResult executeForStockSummary(Consumer<NotificationSetting> task) {
        return execute(
            cursorRequest -> notificationSettingFinder.findAllActive(
                cursorRequest.getCursorId(),
                cursorRequest.toPageable()
            ),
            task,
            "Stock Summary Notification Batch",
            BATCH_SIZE
        );
    }

    @Override
    protected Long getEntityId(NotificationSetting setting) {
        return setting.getId();
    }

}