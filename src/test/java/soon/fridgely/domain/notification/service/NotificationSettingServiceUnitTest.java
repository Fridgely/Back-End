package soon.fridgely.domain.notification.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.notification.dto.request.NotificationSettingUpdateRequest;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.domain.notification.entity.AlertSchedule;
import soon.fridgely.domain.notification.entity.NotificationSetting;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceUnitTest {

    @InjectMocks
    private NotificationSettingService notificationSettingService;

    @Mock
    private NotificationSettingManager notificationSettingManager;

    @Mock
    private NotificationSettingFinder notificationSettingFinder;

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 회원의_알림_설정을_조회한다() {
        // given
        long memberId = 1L;
        var setting = NotificationSetting.createDefaultSetting(mock(Member.class));

        given(notificationSettingFinder.findNotificationSetting(memberId)).willReturn(setting);

        // when
        NotificationSettingDetailResponse response = notificationSettingService.findNotificationSetting(memberId);

        // then
        assertThat(response)
            .extracting("notificationTime", "daysBeforeExpiration", "enabled")
            .containsExactly(LocalTime.of(9, 0), 3, true);
    }

    @Test
    void 회원의_알림_설정을_수정한다() {
        // given
        long memberId = 1L;
        var request = fixtureMonkey.giveMeOne(NotificationSettingUpdateRequest.class);

        // when
        notificationSettingService.updateNotificationSetting(memberId, request);

        // then
        then(notificationSettingManager).should()
            .update(eq(memberId), refEq(AlertSchedule.of(request.notificationTime(), request.daysBeforeExpiration())), eq(request.enabled()));
    }

}