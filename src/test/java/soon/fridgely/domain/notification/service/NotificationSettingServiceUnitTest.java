package soon.fridgely.domain.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.domain.notification.entity.NotificationSetting;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceUnitTest {

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    @Mock
    private NotificationSettingManager notificationManager;

    @Test
    void 회원의_알림_설정을_조회한다() {
        // given
        long memberId = 1L;
        NotificationSetting setting = NotificationSetting.createDefaultSetting(mock(Member.class));

        given(notificationManager.findNotificationSetting(memberId)).willReturn(setting);

        // when
        NotificationSettingDetailResponse response = notificationSettingService.findNotificationSetting(memberId);

        // then
        assertThat(response)
            .extracting("notificationTime", "daysBeforeExpiration", "enabled")
            .containsExactly(LocalTime.of(9, 0), 3, true);
    }

}