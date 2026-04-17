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
import soon.fridgely.global.batch.BatchResult;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

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

    @Captor
    private ArgumentCaptor<List<Long>> deviceIdsCaptor;

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

        // then - 500개 처리 후 최종 flush에서 1번 호출되며 모든 ID가 포함
        then(deviceCleanupProcessor).should().bulkDelete(deviceIdsCaptor.capture());
        assertThat(deviceIdsCaptor.getValue()).hasSize(500);
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

    @Test
    void 처리_건수가_CHUNK_SIZE와_같으면_배치_중_flush는_1회_최종_flush는_없다() {
        // given
        int count = DeviceCleanupProcessor.CHUNK_SIZE;
        BatchResult mockResult = new BatchResult(count, 100L);

        given(deviceCleanupBatchExecutor.executeCleanup(any(LocalDateTime.class), any()))
            .willAnswer(invocation -> {
                Consumer<MemberDevice> task = invocation.getArgument(1);
                for (int i = 0; i < count; i++) {
                    MemberDevice mockDevice = mock(MemberDevice.class);
                    given(mockDevice.getId()).willReturn((long) i);
                    task.accept(mockDevice);
                }
                return mockResult;
            });

        // when
        deviceCleanupScheduler.cleanupInactiveDevices();

        // then - 배치 중 1회만 호출, 최종 flush 없음, 총 1회
        then(deviceCleanupProcessor).should(times(1)).bulkDelete(deviceIdsCaptor.capture());
        assertThat(deviceIdsCaptor.getValue()).hasSize(count);
    }

}