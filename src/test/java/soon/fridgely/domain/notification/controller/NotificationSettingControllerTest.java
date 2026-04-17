package soon.fridgely.domain.notification.controller;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import soon.fridgely.domain.notification.dto.request.NotificationSettingUpdateRequest;
import soon.fridgely.domain.notification.dto.response.NotificationSettingDetailResponse;
import soon.fridgely.global.security.annotation.TestLoginMember;
import soon.fridgely.global.support.ControllerTestSupport;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationSettingControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/notifications/settings";

    @TestLoginMember
    @Test
    void 내_알림_설정을_조회한다() throws Exception {
        // given
        var response = fixtureMonkey.giveMeBuilder(NotificationSettingDetailResponse.class)
            .set("notificationTime", LocalTime.of(9, 0))
            .set("daysBeforeExpiration", 3)
            .set("enabled", true)
            .sample();

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

    @ParameterizedTest
    @MethodSource("provideInvalidUpdateRequests")
    @TestLoginMember
    void 알림_설정_수정_요청_시_필수값이_누락되거나_유효하지_않으면_예외가_발생한다(NotificationSettingUpdateRequest request, String field, String message) throws Exception {
        // expected
        mockMvc.perform(
                patch(BASE_URL)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.error.data.%s", field).value(message));
    }

    private static Stream<Arguments> provideInvalidUpdateRequests() {
        FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();
        return Stream.of(
            Arguments.of(
                fixtureMonkey.giveMeBuilder(NotificationSettingUpdateRequest.class)
                    .validOnly(false)
                    .setNull("notificationTime")
                    .sample(),
                "notificationTime", "알림 시간은 필수입니다."
            ),
            Arguments.of(
                fixtureMonkey.giveMeBuilder(NotificationSettingUpdateRequest.class)
                    .validOnly(false)
                    .set("daysBeforeExpiration", 0)
                    .sample(),
                "daysBeforeExpiration", "알림 기준일은 1일 이상이어야 합니다."
            ),
            Arguments.of(
                fixtureMonkey.giveMeBuilder(NotificationSettingUpdateRequest.class)
                    .validOnly(false)
                    .set("daysBeforeExpiration", -1)
                    .sample(),
                "daysBeforeExpiration", "알림 기준일은 1일 이상이어야 합니다."
            ),
            Arguments.of(
                fixtureMonkey.giveMeBuilder(NotificationSettingUpdateRequest.class)
                    .validOnly(false)
                    .set("daysBeforeExpiration", 31)
                    .sample(),
                "daysBeforeExpiration", "알림 기준일은 최대 30일까지 설정 가능합니다."
            )
        );
    }

}