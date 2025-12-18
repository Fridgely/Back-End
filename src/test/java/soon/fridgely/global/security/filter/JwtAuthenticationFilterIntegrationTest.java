package soon.fridgely.global.security.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import soon.fridgely.domain.auth.provider.TokenProvider;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.global.security.dto.response.TokenResponse;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.security.Key;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static soon.fridgely.global.security.filter.JwtAuthenticationFilter.AUTHORIZATION_HEADER;
import static soon.fridgely.global.security.filter.JwtAuthenticationFilter.BEARER_PREFIX;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureMockMvc
public class JwtAuthenticationFilterIntegrationTest extends IntegrationTestSupport {

    private static final String SECURE_API_PATH = "/api/test/secure";
    private static final String PUBLIC_API_PATH = "/api/test/public";
    private static final long MEMBER_ID = 1L;
    private static final String MEMBER_ROLE = MemberRole.MEMBER.name();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Test
    void 유효한_토큰으로_보호된_API에_접근_할_수_있다() throws Exception {
        // given
        TokenResponse tokenResponse = tokenProvider.generateAllToken(MEMBER_ID, MEMBER_ROLE);

        // expected
        mockMvc.perform(
                get(SECURE_API_PATH).header(AUTHORIZATION_HEADER, BEARER_PREFIX + tokenResponse.accessToken())
            )
            .andExpect(status().isOk());
    }

    @Test
    void 토큰_없이_인증이_필요한_API를_요청하면_401_에러를_반환한다() throws Exception {
        // expected
        mockMvc.perform(get(SECURE_API_PATH))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증이_필요_없는_API는_토큰_없이도_접근할_수_있다() throws Exception {
        mockMvc.perform(get(PUBLIC_API_PATH))
            .andExpect(status().isOk());
    }

    @Test
    void 유효하지_않은_형식의_토큰으로_보호된_API에_접근하면_401_에러를_반환한다() throws Exception {
        // given
        String invalidToken = "this-is-an-invalid-token";

        // expected
        mockMvc.perform(
                get(SECURE_API_PATH)
                    .header(AUTHORIZATION_HEADER, BEARER_PREFIX + invalidToken)
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void Bearer_접두사_없는_토큰으로_보호된_API에_접근하면_401_에러를_반환한다() throws Exception {
        // given
        TokenResponse tokenResponse = tokenProvider.generateAllToken(MEMBER_ID, MEMBER_ROLE);

        // expected
        mockMvc.perform(
                get(SECURE_API_PATH)
                    .header(AUTHORIZATION_HEADER, tokenResponse.accessToken())
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 잘못된_서명_토큰으로_보호된_API에_접근하면_401_에러를_반환한다() throws Exception {
        // given
        String invalidSignatureToken = createTokenWithDifferentKey();

        // expected
        mockMvc.perform(
                get(SECURE_API_PATH)
                    .header(
                        AUTHORIZATION_HEADER,
                        BEARER_PREFIX + invalidSignatureToken
                    )
            )
            .andExpect(status().isUnauthorized());
    }

    /**
     * 테스트용으로, 다른 Secret Key를 사용하여 서명이 위조된 토큰을 생성하는 헬퍼 메서드
     */
    private String createTokenWithDifferentKey() {
        String wrongSecretKey = "VzFhNU5iNXA3YTljVzdoOTcb50lmdEc3M25iQzRzOWs2TzBqSnZnczVGNk5wN3lDNDduM2I5SzFKN0Y1WjJvNA";
        byte[] keyBytes = Decoders.BASE64.decode(wrongSecretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        Date now = new Date();

        return Jwts.builder()
            .setSubject(String.valueOf(MEMBER_ID))
            .claim("auth", MEMBER_ROLE)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + 3600000))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

}