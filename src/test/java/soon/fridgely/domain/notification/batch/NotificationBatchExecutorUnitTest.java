package soon.fridgely.domain.notification.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.domain.notification.service.NotificationSettingFinder;

import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class NotificationBatchExecutorUnitTest {

    @InjectMocks
    private NotificationBatchExecutor executor;

    @Mock
    private NotificationSettingFinder finder;

    @Test
    void 빈_결과인_경우_작업을_실행하지_않는다() {
        // given
        given(finder.findAllActiveByTime(any(), any(), anyLong(), any()))
            .willReturn(new SliceImpl<>(List.of()));

        Consumer<NotificationSetting> task = mockTask();

        // when
        BatchResult result = executor.execute(LocalTime.of(9, 0), LocalTime.of(9, 59), task);

        // then
        assertThat(result.submittedCount()).isZero();
        then(task).shouldHaveNoInteractions();
    }

    @Test
    void 마지막_항목의_ID를_다음_커서로_사용한다() {
        // given
        NotificationSetting setting1 = mock(NotificationSetting.class);
        NotificationSetting setting2 = mock(NotificationSetting.class);
        NotificationSetting setting3 = mock(NotificationSetting.class);

        given(setting2.getId()).willReturn(20L);

        Slice<NotificationSetting> firstPage = new SliceImpl<>(List.of(setting1, setting2), Pageable.ofSize(2), true);
        Slice<NotificationSetting> secondPage = new SliceImpl<>(List.of(setting3), Pageable.ofSize(2), false);

        given(finder.findAllActiveByTime(any(), any(), anyLong(), any()))
            .willReturn(firstPage);
        given(finder.findAllActiveByTime(any(), any(), eq(20L), any()))
            .willReturn(secondPage);

        Consumer<NotificationSetting> task = mockTask();

        // when
        BatchResult result = executor.execute(LocalTime.of(9, 0), LocalTime.of(9, 59), task);

        // then
        then(finder).should()
            .findAllActiveByTime(any(), any(), eq(20L), any());

        assertThat(result.submittedCount()).isEqualTo(3);
        then(task).should(times(3))
            .accept(any());
    }

    @Test
    void 실행_시간을_측정한다() {
        // given
        given(finder.findAllActiveByTime(any(), any(), anyLong(), any()))
            .willReturn(new SliceImpl<>(List.of()));

        // when
        BatchResult result = executor.execute(LocalTime.of(9, 0), LocalTime.of(9, 59), mockTask());

        // then
        assertThat(result.durationMillis()).isNotNegative();
    }

    @SuppressWarnings("unchecked")
    private Consumer<NotificationSetting> mockTask() {
        return mock(Consumer.class);
    }

}
