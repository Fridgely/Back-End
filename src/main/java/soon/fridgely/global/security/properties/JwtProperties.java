package soon.fridgely.global.security.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "spring.jwt")
public record JwtProperties(
    @NotBlank(message = "JWT secretKey는 필수입니다.")
    String secretKey,
    Duration accessTokenExpirationTime,
    Duration refreshTokenExpirationTime
) {

}