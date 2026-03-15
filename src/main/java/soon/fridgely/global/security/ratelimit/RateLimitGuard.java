package soon.fridgely.global.security.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

@RequiredArgsConstructor
@Component
public class RateLimitGuard {

    private final RateLimiterRegistry rateLimiterRegistry;

    public void check(RateLimitInstance instance, String key) {
        RateLimiterConfig config = rateLimiterRegistry.rateLimiter(instance.key()).getRateLimiterConfig();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(instance.key() + "_" + key, config);
        if (!rateLimiter.acquirePermission()) {
            throw new CoreException(ErrorType.TOO_MANY_REQUESTS);
        }
    }

    public String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

}