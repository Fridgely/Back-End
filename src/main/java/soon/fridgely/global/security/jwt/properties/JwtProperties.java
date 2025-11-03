package soon.fridgely.global.security.jwt.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "spring.jwt")
public record JwtProperties(
    String secretKey,
    Duration accessTokenExpirationTime,
    Duration refreshTokenExpirationTime
) {

}