package soon.fridgely.domain.auth.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.auth.controller.dto.request.LoginRequest;
import soon.fridgely.global.security.jwt.dto.response.TokenResponse;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/auth";

    @Test
    void 로그인에_성공한다() throws Exception {
        // given
        var request = new LoginRequest("testId", "testPassword");
        var tokenResponse = new TokenResponse("valid-access-token", "valid-refresh-token");
        given(authService.login(request.toLoginInfo()))
            .willReturn(tokenResponse);

        // expected
        mockMvc.perform(
                post(BASE_URL + "/login")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.accessToken").value("valid-access-token"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidLoginRequests")
    void 로그인_요청_시_필수값이_누락되면_예외가_발생한다(LoginRequest request, String field, String message) throws Exception {
        // expected
        mockMvc.perform(
                post(BASE_URL + "/login")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.error.data.%s", field).value(message));
    }

    @Test
    void 존재하지_않는_ID나_틀린_비밀번호로_로그인하면_401_에러를_반환한다() throws Exception {
        // given
        var request = new LoginRequest("testId", "wrongPassword");

        given(authService.login(request.toLoginInfo()))
            .willThrow(new CoreException(ErrorType.AUTHENTICATION_FAILED));

        // expected
        mockMvc.perform(
                post(BASE_URL + "/login")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.error.message").value("인증에 실패했습니다."));
    }

    private static Stream<Arguments> provideInvalidLoginRequests() {
        return Stream.of(
            Arguments.of(
                new LoginRequest("", "testPassword"),
                "loginId", "ID는 필수입니다."
            ),
            Arguments.of(
                new LoginRequest(null, "testPassword"),
                "loginId", "ID는 필수입니다."
            ),
            Arguments.of(
                new LoginRequest("testId", ""),
                "password", "비밀번호는 필수입니다."
            ),
            Arguments.of(
                new LoginRequest("testId", null),
                "password", "비밀번호는 필수입니다."
            )
        );
    }

}