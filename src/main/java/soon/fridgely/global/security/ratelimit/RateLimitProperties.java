package soon.fridgely.global.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rate-limit.cache")
public record RateLimitProperties(
    int maxSize,
    Duration ttl
) {

}