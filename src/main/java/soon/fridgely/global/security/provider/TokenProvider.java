package soon.fridgely.global.security.provider;

import org.springframework.security.core.Authentication;
import soon.fridgely.global.security.dto.response.TokenResponse;

/**
 * 토큰의 생성, 검증, 정보 추출을 담당하는 인터페이스
 */
public interface TokenProvider {

    /**
     * Access Token과 Refresh Token을 모두 생성합니다.
     */
    TokenResponse generateAllToken(long memberId, String role);

    /**
     * 토큰의 유효성을 검증합니다.
     */
    boolean validateToken(String token);

    /**
     * 토큰을 기반으로 Spring Security의 Authentication 객체를 생성합니다.
     */
    Authentication getAuthentication(String token);

    /**
     * 토큰에서 회원 ID를 추출합니다.
     */
    String getSubjectFromToken(String token);

}