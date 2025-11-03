package soon.fridgely.global.security.jwt.provider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.global.security.jwt.dto.response.TokenResponse;
import soon.fridgely.global.security.jwt.properties.JwtProperties;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.security.Key;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtProviderUnitTest {

    private static final String TEST_SECRET_KEY = "VzFhNU5iNXA3YTljVzdoOTc5b0lmdEc3M25iQzRzOWs2TzBqSnZnczVGNk5wN3lDNDduM2I5SzFKN0Y1WjJvNA==";
    private static final String WRONG_SECRET_KEY = "WrongSecretKeyForTestingPurposeWhichIsVeryLongAndSecureEnoughAndDifferentFromOriginalOne12345";

    @Mock
    private JwtProperties jwtProperties;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        given(jwtProperties.secretKey()).willReturn(TEST_SECRET_KEY);
        given(jwtProperties.accessTokenExpirationTime()).willReturn(Duration.ofMinutes(30));
        given(jwtProperties.refreshTokenExpirationTime()).willReturn(Duration.ofDays(7));
        jwtProvider = new JwtProvider(jwtProperties);
    }

    @Test
    void 멤버_ID와_역할을_받아_토큰을_생성한다() {
        // given
        long memberId = 1L;
        String role = MemberRole.MEMBER.name();

        // when
        TokenResponse tokenResponse = jwtProvider.generateAllToken(memberId, role);

        // then
        assertThat(tokenResponse.accessToken()).isNotNull();
        assertThat(tokenResponse.refreshToken()).isNotNull();
    }

    @Test
    void 액세스_토큰에서_subject를_추출한다() {
        // given
        long memberId = 1L;
        String role = MemberRole.MEMBER.name();
        TokenResponse tokenResponse = jwtProvider.generateAllToken(memberId, role);

        // when
        String subject = jwtProvider.getSubjectFromToken(tokenResponse.accessToken());

        // then
        assertThat(subject).isEqualTo(String.valueOf(memberId));
    }

    @Test
    void 유효한_토큰으로_Authentication_객체를_생성한다() {
        // given
        long memberId = 1L;
        String role = MemberRole.ADMIN.name();
        TokenResponse tokenResponse = jwtProvider.generateAllToken(memberId, role);
        String accessToken = tokenResponse.accessToken();

        // when
        Authentication authentication = jwtProvider.getAuthentication(accessToken);

        // then
        assertThat(authentication.getPrincipal()).isEqualTo(String.valueOf(memberId));

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo(role);
    }

    @Test
    void auth_클레임이_없는_토큰으로_Authentication_객체를_생성하면_예외가_발생한다() {
        // given
        long memberId = 1L;
        String tokenWithoutAuth = createTokenWithoutAuthClaim(memberId, new Date(System.currentTimeMillis() + 10000));

        // expected
        assertThatThrownBy(() -> jwtProvider.getAuthentication(tokenWithoutAuth))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
    }

    @Test
    void 유효한_토큰을_검증하면_true를_반환한다() {
        // given
        TokenResponse tokenResponse = jwtProvider.generateAllToken(1L, "MEMBER");

        // when
        boolean isValid = jwtProvider.validateToken(tokenResponse.accessToken());

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void 만료된_토큰을_검증하면_false를_반환한다() {
        // given
        Date expiration = new Date(System.currentTimeMillis() - 1000); // 1초 전에 만료
        String expiredToken = createToken(expiration, TEST_SECRET_KEY);

        // when
        boolean isValid = jwtProvider.validateToken(expiredToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void 잘못된_서명을_가진_토큰을_검증하면_false를_반환한다() {
        // given
        Date expiration = new Date(System.currentTimeMillis() + 10000); // 10초 후에 만료
        String tokenWithWrongSignature = createToken(expiration, WRONG_SECRET_KEY);

        // when
        boolean isValid = jwtProvider.validateToken(tokenWithWrongSignature);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void 형식에_맞지_않는_토큰을_검증하면_false를_반환한다() {
        // given
        String malformedToken = "this.is.malformed";

        // when
        boolean isValid = jwtProvider.validateToken(malformedToken);

        // then
        assertThat(isValid).isFalse();
    }

    private String createToken(Date expiration, String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
            .setSubject(String.valueOf(1L))
            .claim("auth", MemberRole.MEMBER.name())
            .setExpiration(expiration)
            .setIssuedAt(new Date())
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    private String createTokenWithoutAuthClaim(long memberId, Date expiration) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET_KEY);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
            .setSubject(String.valueOf(memberId))
            .setExpiration(expiration)
            .setIssuedAt(new Date())
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

}