package soon.fridgely.domain.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.global.security.annotation.TestLoginMember;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationSettingControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/notifications/settings";

    @TestLoginMember
    @Test
    void 내_알림_설정을_조회한다() throws Exception {
        // given
        var response = new NotificationSettingDetailResponse(LocalTime.of(9, 0), 3, true);
        given(notificationSettingService.findNotificationSetting(anyLong())).willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.notificationTime").value("09:00:00"))
            .andExpect(jsonPath("$.data.daysBeforeExpiration").value(3))
            .andExpect(jsonPath("$.data.enabled").value(true));
    }

}