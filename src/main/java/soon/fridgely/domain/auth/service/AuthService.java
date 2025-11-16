package soon.fridgely.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import soon.fridgely.domain.auth.dto.command.LoginInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.global.security.dto.response.TokenResponse;
import soon.fridgely.global.security.provider.TokenProvider;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional
    public TokenResponse login(LoginInfo info) {
        Member member = memberRepository.findByLoginId(info.loginId())
            .orElseThrow(() -> new CoreException(ErrorType.AUTHENTICATION_FAILED)); // User Enumeration Attack 방지를 위해 메시지 통일

        if (!passwordEncoder.matches(info.password(), member.getPassword())) {
            throw new CoreException(ErrorType.AUTHENTICATION_FAILED);
        }

        log.info("[login success] memberId = {}", member.getId());

        return issueTokensAndUpdateMember(member);
    }

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        validateRefreshToken(refreshToken);

        long memberId = Long.parseLong(tokenProvider.getSubjectFromToken(refreshToken));
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorType.AUTHENTICATION_FAILED));

        if (!refreshToken.equals(member.getRefreshToken())) {
            throw new CoreException(ErrorType.AUTHENTICATION_FAILED);
        }

        log.info("[reissue success] memberId = {}", memberId);

        return issueTokensAndUpdateMember(member);
    }

    private TokenResponse issueTokensAndUpdateMember(Member member) {
        TokenResponse tokenResponse = tokenProvider.generateAllToken(member.getId(), member.getRole());
        member.updateRefreshToken(tokenResponse.refreshToken());
        return tokenResponse;
    }

    private void validateRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken) || !tokenProvider.validateToken(refreshToken)) {
            throw new CoreException(ErrorType.AUTHENTICATION_FAILED);
        }
    }

}