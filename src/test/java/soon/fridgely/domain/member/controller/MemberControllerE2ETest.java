package soon.fridgely.domain.member.controller;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import soon.fridgely.E2ETestSupport;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.controller.dto.request.MemberRegisterRequest;
import soon.fridgely.domain.member.dto.MemberInfo;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.member.service.MemberService;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.response.ApiResponse;
import soon.fridgely.global.support.response.ResultType;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MemberControllerE2ETest extends E2ETestSupport {

    private static final String BASE_URL = "/api/v1/members";

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void 회원가입_성공_시_201_응답과_멤버_냉장고_연결_기본카테고리가_생성된다() {
        // given
        var request = new MemberRegisterRequest("testId", "testPassword", "testNickname");
        var httpEntity = new HttpEntity<>(request);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Long>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL, HttpMethod.POST, httpEntity, responseType
        );

        // then
        Long memberId = response.getBody().data();

        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
            () -> assertThat(response.getBody().result()).isEqualTo(ResultType.SUCCESS),
            () -> assertThat(memberId).isNotNull()
        );

        assertThat(memberRepository.findById(memberId)).isPresent();
        assertThat(refrigeratorRepository.count()).isEqualTo(1);
        assertThat(memberRefrigeratorRepository.count()).isEqualTo(1);
        assertThat(categoryRepository.count()).isEqualTo(8);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidMemberRegisterRequests")
    void 필수값이_누락된_요청은_예외가_발생한다(MemberRegisterRequest request, String field, String message) {
        // given
        var httpEntity = new HttpEntity<>(request);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Object>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL, HttpMethod.POST, httpEntity, responseType
        );

        // then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
            () -> assertThat(response.getBody().result()).isEqualTo(ResultType.ERROR),
            () -> assertThat(response.getBody().error().message()).isEqualTo(ErrorType.INVALID_REQUEST.getMessage()),
            () -> {
                assertThat(response.getBody().error().data())
                    .asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
                    .containsEntry(field, message);
            }
        );
    }

    @Test
    void 중복되는_ID로_요청시_예외가_발생한다() {
        // given
        var setupInfo = new MemberInfo("testId", "testPassword", "testNickname");
        memberService.register(setupInfo);

        var request = new MemberRegisterRequest("testId", "testPassword", "testNickname");
        var httpEntity = new HttpEntity<>(request);

        // when
        var responseType = new ParameterizedTypeReference<ApiResponse<Object>>() {
        };
        var response = testRestTemplate.exchange(
            BASE_URL, HttpMethod.POST, httpEntity, responseType
        );

        // then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
            () -> assertThat(response.getBody().result()).isEqualTo(ResultType.ERROR),
            () -> assertThat(response.getBody().error().message()).isEqualTo(ErrorType.DUPLICATE_LOGIN_ID.getMessage())
        );
    }

    private static Stream<Arguments> provideInvalidMemberRegisterRequests() {
        return Stream.of(
            Arguments.of(
                new MemberRegisterRequest(null, "password", "nickname"),
                "loginId", "ID는 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("", "password", "nickname"),
                "loginId", "ID는 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("testId", null, "nickname"),
                "password", "비밀번호는 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("testId", "", "nickname"),
                "password", "비밀번호는 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("testId", "password", null),
                "nickname", "닉네임은 필수입니다."
            ),
            Arguments.of(
                new MemberRegisterRequest("testId", "password", ""),
                "nickname", "닉네임은 필수입니다."
            )
        );
    }

}