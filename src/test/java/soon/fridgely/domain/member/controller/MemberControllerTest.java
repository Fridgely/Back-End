package soon.fridgely.domain.member.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import soon.fridgely.ControllerTestSupport;
import soon.fridgely.domain.member.dto.request.MemberRegisterRequest;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/members";

    @Test
    void 회원을_등록한다() throws Exception {
        // given
        var request = new MemberRegisterRequest("testId", "testPassword", "testNickname");

        given(memberService.register(request.toInfo()))
            .willReturn(1L);

        // expected
        mockMvc.perform(
                post(BASE_URL)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data").value(1));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidMemberRegisterRequests")
    void 회원_등록시_필수값이_누락되면_예외가_발생한다(MemberRegisterRequest request, String field, String message) throws Exception {
        // expected
        mockMvc.perform(
                post(BASE_URL)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.error.message").value("요청이 올바르지 않습니다."))
            .andExpect(jsonPath("$.error.data.%s", field).value(message));
    }

    @Test
    void 중복된_ID로_회원_등록시_예외가_발생한다() throws Exception {
        // given
        var request = new MemberRegisterRequest("duplicateId", "password", "nickname");

        given(memberService.register(request.toInfo()))
            .willThrow(new CoreException(ErrorType.DUPLICATE_LOGIN_ID));

        // expected
        mockMvc.perform(
                post(BASE_URL)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.result").value("ERROR"));
    }

    private static Stream<Arguments> provideInvalidMemberRegisterRequests() {
        return Stream.of(
            Arguments.of(
                new MemberRegisterRequest(null, "password", "nickname"),
                "loginId", "ID는 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("testId", null, "nickname"),
                "password", "비밀번호는 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("testId", "password", null),
                "nickname", "닉네임은 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("", "password", "nickname"),
                "loginId", "ID는 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("testId", "", "nickname"),
                "password", "비밀번호는 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("testId", "password", ""),
                "nickname", "닉네임은 필수입니다."
            )
        );
    }

}