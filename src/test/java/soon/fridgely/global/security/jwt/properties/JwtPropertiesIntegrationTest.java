package soon.fridgely.global.security.jwt.properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.IntegrationTestSupport;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JwtProperties jwtProperties;

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