package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.domain.notification.batch.NotificationBatchExecutor;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationBatchExecutor notificationBatchExecutor;

    @Mock
    private NotificationProcessor notificationProcessor;

    @Test
    void 배치를_실행하고_결과를_로그로_남긴다() {
        // given
        BatchResult mockResult = BatchResult.of(100, 500L);
        given(notificationBatchExecutor.execute(any(LocalTime.class), any(LocalTime.class), any()))
            .willReturn(mockResult);

        // when
        notificationService.sendScheduledAlerts();

        // then
        then(notificationBatchExecutor).should()
            .execute(any(LocalTime.class), any(LocalTime.class), any());
    }

}