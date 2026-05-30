package soon.fridgely.global.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class UploadRateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${upload.rate-limit.max-requests}")
    private int maxRequests;

    @Value("${upload.rate-limit.period-seconds}")
    private long periodSeconds;

    @Before("@annotation(soon.fridgely.global.security.ratelimit.UploadRateLimit)")
    public void checkRateLimit() {
        Long userId = extractUserId();
        String key = "upload:ratelimit:" + userId;

        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1L).equals(count)) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(periodSeconds));
        }
        if (count != null && count > maxRequests) {
            log.warn("[UploadRateLimitAspect] 업로드 Rate Limit 초과. (UserId={}, Count={})", userId, count);
            throw new CoreException(ErrorType.UPLOAD_RATE_LIMIT_EXCEEDED);
        }
    }

    private Long extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new CoreException(ErrorType.AUTHENTICATION_FAILED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                throw new CoreException(ErrorType.AUTHENTICATION_FAILED);
            }
        }
        throw new CoreException(ErrorType.AUTHENTICATION_FAILED);
    }
}
