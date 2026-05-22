package soon.fridgely.domain.notification.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.service.NotificationSettingService;
import soon.fridgely.global.batch.AbstractBatchExecutor;
import soon.fridgely.global.batch.BatchResult;

import java.time.LocalTime;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationBatchExecutor extends AbstractBatchExecutor<NotificationSetting> {

    private static final int BATCH_SIZE = 100;
    private final NotificationSettingService notificationSettingService;

    public BatchResult executeForExpiration(
        LocalTime startTime,
        LocalTime endTime,
        Consumer<NotificationSetting> task
    ) {
        return execute(
            cursorRequest -> notificationSettingService.findAllActiveByTime(
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

    public BatchResult executeForStockSummary(Consumer<NotificationSetting> task) {
        return execute(
            cursorRequest -> notificationSettingService.findAllActive(
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