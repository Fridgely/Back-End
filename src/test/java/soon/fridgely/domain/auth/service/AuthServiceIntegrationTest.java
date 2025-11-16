package soon.fridgely.domain.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.auth.dto.command.LoginInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.global.security.dto.response.TokenResponse;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 로그인에_성공한다() {
        // given
        Member member = createMember();
        Member savedMember = memberRepository.save(member);

        var info = new LoginInfo("testId", "testPassword");

        // when
        TokenResponse response = authService.login(info);

        // then
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();

        Member updated = memberRepository.findById(savedMember.getId()).orElse(null);
        assertThat(updated.getRefreshToken())
            .isNotNull()
            .isEqualTo(response.refreshToken());
    }

    @Test
    void 존재하지_않는_아이디로_로그인하면_예외가_발생한다() {
        // given
        var info = new LoginInfo("notExistId", "anyPassword");

        // expected
        assertThatThrownBy(() -> authService.login(info))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHENTICATION_FAILED);
    }

    @Test
    void 비밀번호가_일치하지_않으면_로그인에_실패한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        var info = new LoginInfo("testId", "wrongPassword");

        // expected
        assertThatThrownBy(() -> authService.login(info))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHENTICATION_FAILED);
    }

    @Test
    void 유효한_Refresh_Token으로_토큰_재발급에_성공한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        var tokens = authService.login(new LoginInfo("testId", "testPassword"));

        // when
        var newTokens = authService.reissue(tokens.refreshToken());

        // then
        assertThat(newTokens.refreshToken()).isNotEqualTo(tokens.refreshToken());

        Member updatedMember = memberRepository.findById(member.getId()).orElse(null);
        assertThat(updatedMember.getRefreshToken())
            .isNotNull()
            .isEqualTo(newTokens.refreshToken());
    }

    @Test
    void 만료되거나_유효하지_않은_토큰은_예외가_발생한다() {
        // given
        String invalidToken = "invalidToken";

        // expected
        assertThatThrownBy(() -> authService.reissue(invalidToken))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHENTICATION_FAILED);
    }

    @Test
    void 유효한_토큰이지만_존재하지_않는_사용자는_예외가_발생한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        var tokens = authService.login(new LoginInfo("testId", "testPassword"));
        memberRepository.delete(member);

        // expected
        assertThatThrownBy(() -> authService.reissue(tokens.refreshToken()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHENTICATION_FAILED);
    }

    @Test
    void DB와_일치하지_않는_토큰은_예외가_발생한다() {
        // given
        Member member = createMember();
        memberRepository.save(member);

        var tokens = authService.login(new LoginInfo("testId", "testPassword"));
        authService.login(new LoginInfo("testId", "testPassword")); // DB 토큰 갱신

        // expected
        assertThatThrownBy(() -> authService.reissue(tokens.refreshToken()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHENTICATION_FAILED);
    }

    private Member createMember() {
        return Member.register(
            "testId",
            "testPassword",
            "testNickname",
            MemberRole.MEMBER,
            passwordEncoder
        );
    }

}