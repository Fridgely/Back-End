package soon.fridgely.global.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.logging.SlackMarkers;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class LoginRateLimitAspect {

    private static final int MAX_REQUESTS = 10;
    private static final long PERIOD_SECONDS = 60L;

    private final StringRedisTemplate stringRedisTemplate;

    @Before("@annotation(soon.fridgely.global.security.ratelimit.LoginRateLimit)")
    public void checkRateLimit() {
        String ip = extractClientIp();
        String key = "login:ratelimit:" + ip;

        Long count = stringRedisTemplate.opsForValue().increment(key);
        Long ttl = stringRedisTemplate.getExpire(key);
        if (ttl != null && ttl == -1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(PERIOD_SECONDS));
        }
        if (count != null && count > MAX_REQUESTS) {
            log.warn(SlackMarkers.SYSTEM, "[LoginRateLimitAspect] 로그인 Rate Limit 초과. (IP={})", ip);
            throw new CoreException(ErrorType.LOGIN_RATE_LIMIT_EXCEEDED);
        }
    }

    private String extractClientIp() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }
}
