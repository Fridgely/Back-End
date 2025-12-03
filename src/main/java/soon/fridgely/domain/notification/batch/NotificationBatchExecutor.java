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

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationBatchExecutor {

    private static final int BATCH_SIZE = 100;
    private final NotificationSettingFinder notificationSettingFinder;

    /**
     * 지정된 시간 범위 내의 활성화된 알림 설정에 대해 주어진 작업을 배치로 실행합니다.
     *
     * @return 총 처리(요청)된 건수
     */
    public BatchResult execute(LocalTime startTime, LocalTime endTime, Consumer<NotificationSetting> task) {
        StopWatch stopWatch = new StopWatch("Notification Batch Executor");
        stopWatch.start();

        Long cursorId = null;
        int totalRequest = 0;

        while (true) {
            CursorPageRequest cursorRequest = new CursorPageRequest(cursorId, BATCH_SIZE);
            Slice<NotificationSetting> slice = notificationSettingFinder.findAllActiveByTime(
                startTime, endTime, cursorRequest.getCursorId(), cursorRequest.toPageable()
            );

            if (slice.isEmpty()) {
                break;
            }

            slice.forEach(task);
            totalRequest += slice.getNumberOfElements();

            if (!slice.hasNext()) {
                break;
            }

            cursorId = slice.getContent().get(slice.getNumberOfElements() - 1).getId();
        }

        stopWatch.stop();
        return BatchResult.of(totalRequest, stopWatch.getTotalTimeMillis());
    }

}