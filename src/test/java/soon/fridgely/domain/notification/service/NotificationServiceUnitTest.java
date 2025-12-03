package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import soon.fridgely.domain.notification.entity.NotificationSetting;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationSettingFinder notificationSettingFinder;

    @Mock
    private NotificationProcessor notificationProcessor;

    @Test
    void 활성화된_알림_설정이_있으면_알림을_발송한다() {
        // given
        NotificationSetting setting = mock(NotificationSetting.class);
        Slice<NotificationSetting> slice = new SliceImpl<>(List.of(setting), Pageable.unpaged(), false);
        given(notificationSettingFinder.findAllActiveByTime(any(LocalTime.class), any(LocalTime.class), anyLong(), any(Pageable.class)))
            .willReturn(slice);

        // when
        notificationService.sendScheduledAlerts();

        // then
        then(notificationProcessor).should()
            .process(setting);
    }

    @Test
    void 활성화된_알림_설정이_없으면_알림을_발송하지_않는다() {
        // given
        Slice<NotificationSetting> emptySlice = new SliceImpl<>(List.of(), Pageable.unpaged(), false);
        given(notificationSettingFinder.findAllActiveByTime(any(LocalTime.class), any(LocalTime.class), anyLong(), any(Pageable.class)))
            .willReturn(emptySlice);

        // when
        notificationService.sendScheduledAlerts();

        // then
        then(notificationProcessor).should(never())
            .process(any());
    }

    @Test
    void 여러_페이지의_알림_설정을_모두_처리한다() {
        // given
        NotificationSetting setting1 = mock(NotificationSetting.class);
        NotificationSetting setting2 = mock(NotificationSetting.class);
        NotificationSetting setting3 = mock(NotificationSetting.class);

        given(setting2.getId()).willReturn(50L);

        Slice<NotificationSetting> firstPage = new SliceImpl<>(List.of(setting1, setting2), Pageable.unpaged(), true);
        given(notificationSettingFinder.findAllActiveByTime(any(LocalTime.class), any(LocalTime.class), eq(Long.MAX_VALUE), any(Pageable.class)))
            .willReturn(firstPage);
        Slice<NotificationSetting> secondPage = new SliceImpl<>(List.of(setting3), Pageable.unpaged(), false);
        given(notificationSettingFinder.findAllActiveByTime(any(LocalTime.class), any(LocalTime.class), eq(50L), any(Pageable.class)))
            .willReturn(secondPage);

        // when
        notificationService.sendScheduledAlerts();

        // then
        then(notificationProcessor).should(times(3))
            .process(any(NotificationSetting.class));
    }

    @Test
    void cursor_기반_페이징으로_다음_페이지를_조회한다() {
        // given
        NotificationSetting setting1 = mock(NotificationSetting.class);
        NotificationSetting setting2 = mock(NotificationSetting.class);
        NotificationSetting setting3 = mock(NotificationSetting.class);

        given(setting2.getId()).willReturn(50L);

        Slice<NotificationSetting> firstPage = new SliceImpl<>(List.of(setting1, setting2), Pageable.unpaged(), true);
        given(notificationSettingFinder.findAllActiveByTime(any(LocalTime.class), any(LocalTime.class), eq(Long.MAX_VALUE), any(Pageable.class)))
            .willReturn(firstPage);

        Slice<NotificationSetting> secondPage = new SliceImpl<>(List.of(setting3), Pageable.unpaged(), false);
        given(notificationSettingFinder.findAllActiveByTime(any(LocalTime.class), any(LocalTime.class), eq(50L), any(Pageable.class)))
            .willReturn(secondPage);

        // when
        notificationService.sendScheduledAlerts();

        // then
        then(notificationSettingFinder).should(times(2))
            .findAllActiveByTime(any(LocalTime.class), any(LocalTime.class), anyLong(), any(Pageable.class));
    }

}