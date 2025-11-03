package soon.fridgely.global.security.jwt.properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.TestSecurityConfig;
import soon.fridgely.global.security.jwt.provider.TokenProvider;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestSecurityConfig.class)
class JwtPropertiesIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private TokenProvider tokenProvider;

    @Test
    void 설정파일의_JWT_프로퍼티가_정상적으로_바인딩된다() {
        // given
        Duration expectedAccessTokenExpiration = Duration.ofMinutes(30);
        Duration expectedRefreshTokenExpiration = Duration.ofDays(7);

        // expected
        assertThat(jwtProperties.secretKey()).isNotNull();
        assertThat(jwtProperties.accessTokenExpirationTime()).isEqualTo(expectedAccessTokenExpiration);
        assertThat(jwtProperties.refreshTokenExpirationTime()).isEqualTo(expectedRefreshTokenExpiration);
    }

}