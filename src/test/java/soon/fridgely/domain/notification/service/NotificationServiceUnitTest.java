package soon.fridgely.domain.notification.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.domain.notification.batch.NotificationBatchExecutor;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalTime;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationBatchExecutor notificationBatchExecutor;

    @Mock
    private NotificationProcessor notificationProcessor;

    @Captor
    private ArgumentCaptor<Consumer<NotificationSetting>> taskCaptor;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 유통기한_임박_알림_배치를_실행한다() {
        // given
        BatchResult mockResult = fixtureMonkey.giveMeOne(BatchResult.class);
        given(notificationBatchExecutor.executeForExpiration(any(LocalTime.class), any(LocalTime.class), any()))
            .willReturn(mockResult);

        // when
        BatchResult result = notificationService.sendScheduledAlerts();

        // then
        assertThat(result).isEqualTo(mockResult);
        then(notificationBatchExecutor).should()
            .executeForExpiration(any(LocalTime.class), any(LocalTime.class), any());
    }

    @Test
    void 재고_소진_알림_배치를_실행한다() {
        // given
        var mockResult = fixtureMonkey.giveMeOne(BatchResult.class);
        given(notificationBatchExecutor.executeForStockSummary(any()))
            .willReturn(mockResult);

        // when
        BatchResult result = notificationService.sendOutOfStockSummaries();

        // then
        then(notificationBatchExecutor).should()
            .executeForStockSummary(taskCaptor.capture());
        assertThat(result).isEqualTo(mockResult);

        verifyTaskLogic(taskCaptor.getValue());
    }

    private void verifyTaskLogic(Consumer<NotificationSetting> task) {
        // given
        NotificationSetting setting = mock(NotificationSetting.class);
        Member member = mock(Member.class);
        long memberId = 123L;

        given(setting.getMember()).willReturn(member);
        given(member.getId()).willReturn(memberId);

        // when
        task.accept(setting);

        // then
        then(notificationProcessor).should().processStockSummary(memberId);
    }

}