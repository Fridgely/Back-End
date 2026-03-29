package soon.fridgely.global.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@Component
public class RateLimitGuard {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final Cache<String, RateLimiter> rateLimiterCache;

    public RateLimitGuard(RateLimiterRegistry rateLimiterRegistry, RateLimitProperties rateLimitProperties) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.rateLimiterCache = Caffeine.newBuilder()
            .maximumSize(rateLimitProperties.maxSize())
            .expireAfterWrite(rateLimitProperties.ttl())
            .build();
    }

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