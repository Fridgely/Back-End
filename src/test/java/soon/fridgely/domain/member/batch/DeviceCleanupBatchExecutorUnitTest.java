package soon.fridgely.domain.member.batch;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.MemberDevice;
import soon.fridgely.domain.notification.batch.BatchResult;
import soon.fridgely.domain.notification.repository.MemberDeviceRepository;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceCleanupBatchExecutorUnitTest {

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @InjectMocks
    private DeviceCleanupBatchExecutor executor;

    @Mock
    private MemberDeviceRepository memberDeviceRepository;

    @Test
    void 빈_결과인_경우_작업을_실행하지_않는다() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        given(memberDeviceRepository.findInactiveDevices(any(), eq(threshold), nullable(Long.class), any()))
            .willReturn(new SliceImpl<>(List.of()));

        Consumer<MemberDevice> task = mockTask();

        // when
        BatchResult result = executor.executeCleanup(threshold, task);

        // then
        assertThat(result.submittedCount()).isZero();
        then(task).shouldHaveNoInteractions();
    }

    @Test
    void 장기_미사용_디바이스를_순회하며_작업을_수행한다() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);

        MemberDevice device1 = fixtureMonkey.giveMeBuilder(MemberDevice.class)
            .set("id", 1L)
            .sample();
        MemberDevice device2 = fixtureMonkey.giveMeBuilder(MemberDevice.class)
            .set("id", 2L)
            .sample();

        Slice<MemberDevice> page = new SliceImpl<>(List.of(device1, device2), Pageable.ofSize(2), false);

        given(memberDeviceRepository.findInactiveDevices(eq(EntityStatus.ACTIVE), eq(threshold), nullable(Long.class), any()))
            .willReturn(page);

        Consumer<MemberDevice> task = mockTask();

        // when
        BatchResult result = executor.executeCleanup(threshold, task);

        // then
        assertThat(result.submittedCount()).isEqualTo(2);
        then(task).should(times(2)).accept(any());
    }

    @Test
    void 마지막_항목의_ID를_다음_커서로_사용한다() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);

        MemberDevice device1 = fixtureMonkey.giveMeBuilder(MemberDevice.class)
            .set("id", 10L)
            .sample();
        MemberDevice device2 = fixtureMonkey.giveMeBuilder(MemberDevice.class)
            .set("id", 20L)
            .sample();
        MemberDevice device3 = fixtureMonkey.giveMeBuilder(MemberDevice.class)
            .set("id", 30L)
            .sample();

        Slice<MemberDevice> firstPage = new SliceImpl<>(List.of(device1, device2), Pageable.ofSize(2), true);
        Slice<MemberDevice> secondPage = new SliceImpl<>(List.of(device3), Pageable.ofSize(2), false);

        given(memberDeviceRepository.findInactiveDevices(eq(EntityStatus.ACTIVE), eq(threshold), nullable(Long.class), any()))
            .willReturn(firstPage);
        given(memberDeviceRepository.findInactiveDevices(eq(EntityStatus.ACTIVE), eq(threshold), eq(20L), any()))
            .willReturn(secondPage);

        Consumer<MemberDevice> task = mockTask();

        // when
        BatchResult result = executor.executeCleanup(threshold, task);

        // then

        assertThat(result.submittedCount()).isEqualTo(3);
        then(task).should(times(3)).accept(any());
    }

    @Test
    void 작업_실패시_다음_디바이스를_계속_처리한다() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);

        MemberDevice device1 = fixtureMonkey.giveMeBuilder(MemberDevice.class)
            .set("id", 1L)
            .sample();
        MemberDevice device2 = fixtureMonkey.giveMeBuilder(MemberDevice.class)
            .set("id", 2L)
            .sample();

        Slice<MemberDevice> page = new SliceImpl<>(List.of(device1, device2), Pageable.ofSize(2), false);

        given(memberDeviceRepository.findInactiveDevices(eq(EntityStatus.ACTIVE), eq(threshold), nullable(Long.class), any()))
            .willReturn(page);

        Consumer<MemberDevice> task = mock(Consumer.class);
        doThrow(new RuntimeException("테스트 예외")).when(task).accept(device1);

        // when
        BatchResult result = executor.executeCleanup(threshold, task);

        // then
        // 첫 번째 디바이스 실패해도 두 번째는 계속 처리
        assertThat(result.submittedCount()).isEqualTo(1);
        then(task).should(times(2)).accept(any());
    }

    @SuppressWarnings("unchecked")
    private Consumer<MemberDevice> mockTask() {
        return mock(Consumer.class);
    }

}