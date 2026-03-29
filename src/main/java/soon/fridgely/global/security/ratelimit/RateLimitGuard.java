package soon.fridgely.global.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class RateLimitGuard {

    private static final int MAX_RATE_LIMITER_CACHE_SIZE = 10_000;
    private static final Duration RATE_LIMITER_CACHE_TTL = Duration.ofMinutes(2);

    private final RateLimiterRegistry rateLimiterRegistry;

    private final Cache<String, RateLimiter> rateLimiterCache = Caffeine.newBuilder()
        .maximumSize(MAX_RATE_LIMITER_CACHE_SIZE)
        .expireAfterWrite(RATE_LIMITER_CACHE_TTL)
        .build();

    public void check(RateLimitInstance instance, String key) {
        String compositeName = instance.key() + "_" + key;
        RateLimiter rateLimiter = rateLimiterCache.get(compositeName, k -> {
            RateLimiterConfig config = rateLimiterRegistry.rateLimiter(instance.key()).getRateLimiterConfig();
            return RateLimiter.of(compositeName, config);
        });
        if (!rateLimiter.acquirePermission()) {
            throw new CoreException(ErrorType.TOO_MANY_REQUESTS);
        }
    }

    public String extractClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

}