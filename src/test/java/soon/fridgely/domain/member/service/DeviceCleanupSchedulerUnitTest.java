package soon.fridgely.domain.member.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.member.batch.DeviceCleanupBatchExecutor;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DeviceCleanupSchedulerUnitTest {

    @InjectMocks
    private DeviceCleanupScheduler deviceCleanupScheduler;

    @Mock
    private DeviceCleanupBatchExecutor deviceCleanupBatchExecutor;

    @Mock
    private DeviceCleanupProcessor deviceCleanupProcessor;

    @Captor
    private ArgumentCaptor<LocalDateTime> thresholdCaptor;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 장기_미사용_디바이스_정리_배치를_실행한다() {
        // given
        BatchResult mockResult = fixtureMonkey.giveMeOne(BatchResult.class);
        given(deviceCleanupBatchExecutor.executeCleanup(any(LocalDateTime.class), any()))
            .willReturn(mockResult);

        // when
        BatchResult result = deviceCleanupScheduler.cleanupInactiveDevices();

        // then
        assertThat(result).isEqualTo(mockResult);
        then(deviceCleanupBatchExecutor).should()
            .executeCleanup(thresholdCaptor.capture(), any());

        // 90일 전 기준으로 정리
        LocalDateTime threshold = thresholdCaptor.getValue();
        assertThat(threshold).isBefore(LocalDateTime.now().minusDays(89));
        assertThat(threshold).isAfter(LocalDateTime.now().minusDays(91));
    }

    @Test
    void 배치_실행_후_벌크_삭제가_호출된다() {
        // given
        BatchResult mockResult = new BatchResult(500, 100L);
        given(deviceCleanupBatchExecutor.executeCleanup(any(LocalDateTime.class), any()))
            .willAnswer(invocation -> {
                Consumer<MemberDevice> task = invocation.getArgument(1);
                for (int i = 0; i < 500; i++) {
                    MemberDevice mockDevice = mock(MemberDevice.class);
                    given(mockDevice.getId()).willReturn((long) i);
                    task.accept(mockDevice);
                }
                return mockResult;
            });

        // when
        deviceCleanupScheduler.cleanupInactiveDevices();

        // then - 500개 처리 후 최종 flush에서 1번 호출
        then(deviceCleanupProcessor).should().bulkDelete(anyList());
    }

    @Test
    void 처리할_디바이스가_없으면_벌크_삭제가_호출되지_않는다() {
        // given
        BatchResult mockResult = new BatchResult(0, 10L);
        given(deviceCleanupBatchExecutor.executeCleanup(any(LocalDateTime.class), any()))
            .willReturn(mockResult);

        // when
        deviceCleanupScheduler.cleanupInactiveDevices();

        // then
        then(deviceCleanupProcessor).should(never()).bulkDelete(anyList());
    }

}