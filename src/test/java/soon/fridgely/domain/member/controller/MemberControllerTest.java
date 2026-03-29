package soon.fridgely.domain.member.controller;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.domain.member.dto.request.DeviceTokenSyncRequest;
import soon.fridgely.domain.member.dto.request.MemberRegisterRequest;
import soon.fridgely.global.security.annotation.TestLoginMember;
import soon.fridgely.global.security.ratelimit.RateLimitInstance;
import soon.fridgely.global.support.ControllerTestSupport;
import soon.fridgely.global.support.FixtureMonkeyFactory;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest extends ControllerTestSupport {

    private static final String BASE_URL = "/api/v1/members";

    @Test
    void 회원을_등록한다() throws Exception {
        // given
        var request = fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
            .set("loginId", "testId")
            .set("password", "testPassword")
            .set("nickname", "testNickname")
            .sample();

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
    void 회원_등록_요청이_제한_횟수를_초과하면_예외가_발생한다() throws Exception {
        // given
        var request = fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
            .set("loginId", "testId")
            .set("password", "testPassword")
            .set("nickname", "testNickname")
            .sample();
        willThrow(new CoreException(ErrorType.TOO_MANY_REQUESTS))
            .given(rateLimitGuard).check(eq(RateLimitInstance.REGISTER), any());

        // expected
        mockMvc.perform(
                post(BASE_URL)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.error.message").value("요청이 너무 많습니다. 잠시 후 다시 시도해주세요."));
    }

    @Test
    void 중복된_ID로_회원_등록시_예외가_발생한다() throws Exception {
        // given
        var request = fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
            .set("loginId", "duplicateId")
            .set("password", "password")
            .set("nickname", "nickname")
            .sample();

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

    @TestLoginMember
    @Test
    void 디바이스_토큰을_동기화한다() throws Exception {
        // given
        var request = fixtureMonkey.giveMeBuilder(DeviceTokenSyncRequest.class)
            .set("token", "fcm-token-12345")
            .sample();

        // expected
        mockMvc.perform(
                put(BASE_URL + "/me/devices")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(memberService).syncToken(1L, request.token());
    }

    @TestLoginMember
    @Test
    void 마이페이지_프로필을_조회한다() throws Exception {
        // given
        var response = fixtureMonkey.giveMeBuilder(soon.fridgely.domain.member.dto.response.MemberProfileResponse.class)
            .set("loginId", "testId")
            .set("nickname", "testNickname")
            .set("profileImageUrl", "https://s3.amazonaws.com/bucket/images/profile.jpg")
            .sample();

        given(memberService.getMyProfile(1L)).willReturn(response);

        // expected
        mockMvc.perform(
                get(BASE_URL + "/me")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.loginId").value("testId"))
            .andExpect(jsonPath("$.data.nickname").value("testNickname"))
            .andExpect(jsonPath("$.data.profileImageUrl").value("https://s3.amazonaws.com/bucket/images/profile.jpg"));
    }

    @TestLoginMember
    @Test
    void 프로필_이미지를_업로드한다() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "profile.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "image-bytes".getBytes()
        );

        // expected
        mockMvc.perform(
                multipart(HttpMethod.PATCH, BASE_URL + "/me/profile-image")
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(memberService).updateProfileImage(eq(1L), any());
    }

    @TestLoginMember
    @Test
    void 파일_업로드_실패_시_예외가_발생한다() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "profile.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "image-bytes".getBytes()
        );
        willThrow(new CoreException(ErrorType.STORAGE_UPLOAD_FAILED))
            .given(memberService).updateProfileImage(anyLong(), any());

        // expected
        mockMvc.perform(
                multipart(HttpMethod.PATCH, BASE_URL + "/me/profile-image")
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
            )
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.result").value("ERROR"));
    }

    @TestLoginMember
    @Test
    void 디바이스_토큰이_없으면_예외가_발생한다() throws Exception {
        // given
        var request = new DeviceTokenSyncRequest("");

        // expected
        mockMvc.perform(
                put(BASE_URL + "/me/devices")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.result").value("ERROR"))
            .andExpect(jsonPath("$.error.message").value("요청이 올바르지 않습니다."))
            .andExpect(jsonPath("$.error.data.token").value("토큰은 필수입니다."));
    }

    private static Stream<Arguments> provideInvalidMemberRegisterRequests() {
        FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();
        return Stream.of(
            Arguments.of(
                fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
                    .validOnly(false)
                    .set("loginId", null)
                    .sample(),
                "loginId", "ID는 필수입니다."
            ),
            Arguments.of(
                fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
                    .validOnly(false)
                    .set("loginId", "")
                    .sample(),
                "loginId", "ID는 필수입니다."
            ),
            Arguments.of(
                fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
                    .validOnly(false)
                    .set("password", null)
                    .sample(),
                "password", "비밀번호는 필수입니다."
            ),
            Arguments.of(
                fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
                    .validOnly(false)
                    .set("password", "")
                    .sample(),
                "password", "비밀번호는 필수입니다."
            ),
            Arguments.of(
                fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
                    .validOnly(false)
                    .set("nickname", null)
                    .sample(),
                "nickname", "닉네임은 필수입니다."
            ),
            Arguments.of(
                fixtureMonkey.giveMeBuilder(MemberRegisterRequest.class)
                    .validOnly(false)
                    .set("nickname", "")
                    .sample(),
                "nickname", "닉네임은 필수입니다."
            )
        );
    }

}