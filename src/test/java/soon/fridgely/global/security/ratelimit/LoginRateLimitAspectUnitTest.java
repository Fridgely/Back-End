package soon.fridgely.global.security.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitAspectUnitTest {

    @InjectMocks
    private LoginRateLimitAspect aspect;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 요청_횟수가_한도_이하이면_예외가_발생하지_않는다() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login:ratelimit:192.168.1.1")).willReturn(5L);
        given(stringRedisTemplate.getExpire("login:ratelimit:192.168.1.1")).willReturn(30L);

        // when & then
        assertThatCode(() -> aspect.checkRateLimit()).doesNotThrowAnyException();
    }

    @Test
    void TTL이_없는_키는_60초로_expire를_설정한다() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login:ratelimit:192.168.1.1")).willReturn(1L);
        given(stringRedisTemplate.getExpire("login:ratelimit:192.168.1.1")).willReturn(-1L);

        // when
        aspect.checkRateLimit();

        // then
        then(stringRedisTemplate).should().expire(eq("login:ratelimit:192.168.1.1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void TTL이_설정된_키는_expire를_재설정하지_않는다() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login:ratelimit:192.168.1.1")).willReturn(5L);
        given(stringRedisTemplate.getExpire("login:ratelimit:192.168.1.1")).willReturn(30L);

        // when
        aspect.checkRateLimit();

        // then
        then(stringRedisTemplate).should(never()).expire(any(), any());
    }

    @Test
    void 요청_횟수가_한도를_초과하면_예외가_발생한다() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login:ratelimit:192.168.1.1")).willReturn(11L);

        // expected
        assertThatThrownBy(() -> aspect.checkRateLimit())
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.LOGIN_RATE_LIMIT_EXCEEDED);
    }

    @Test
    void 신뢰_프록시에서_온_요청은_XForwardedFor_첫번째_IP를_Rate_Limit_키로_사용한다() {
        // given: MockHttpServletRequest 기본 remoteAddr = "127.0.0.1"
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ReflectionTestUtils.setField(aspect, "trustedProxiesConfig", "127.0.0.1");

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login:ratelimit:10.0.0.1")).willReturn(5L);
        given(stringRedisTemplate.getExpire("login:ratelimit:10.0.0.1")).willReturn(30L);

        // when
        aspect.checkRateLimit();

        // then
        then(valueOperations).should().increment("login:ratelimit:10.0.0.1");
        then(stringRedisTemplate).should().getExpire("login:ratelimit:10.0.0.1");
    }

    @Test
    void 신뢰_프록시에서_온_요청은_XRealIP를_Rate_Limit_키로_사용한다() {
        // given: MockHttpServletRequest 기본 remoteAddr = "127.0.0.1"
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "10.0.0.3");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ReflectionTestUtils.setField(aspect, "trustedProxiesConfig", "127.0.0.1");

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login:ratelimit:10.0.0.3")).willReturn(5L);
        given(stringRedisTemplate.getExpire("login:ratelimit:10.0.0.3")).willReturn(30L);

        // when
        aspect.checkRateLimit();

        // then
        then(valueOperations).should().increment("login:ratelimit:10.0.0.3");
        then(stringRedisTemplate).should().getExpire("login:ratelimit:10.0.0.3");
    }

    @Test
    void 비신뢰_프록시에서_전달된_XForwardedFor_헤더는_무시하고_remoteAddr를_사용한다() {
        // given: trustedProxiesConfig 미설정 (기본값 빈 문자열) → 헤더 무시
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        request.addHeader("X-Forwarded-For", "5.6.7.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login:ratelimit:1.2.3.4")).willReturn(5L);
        given(stringRedisTemplate.getExpire("login:ratelimit:1.2.3.4")).willReturn(30L);

        // when
        aspect.checkRateLimit();

        // then: X-Forwarded-For의 5.6.7.8이 아닌 remoteAddr 1.2.3.4로 카운팅
        then(valueOperations).should().increment("login:ratelimit:1.2.3.4");
        then(stringRedisTemplate).should().getExpire("login:ratelimit:1.2.3.4");
    }

    @Test
    void 헤더가_없으면_remoteAddr를_Rate_Limit_키로_사용한다() {
        // given: setUp()에서 remoteAddr = "192.168.1.1", trustedProxiesConfig 미설정
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("login:ratelimit:192.168.1.1")).willReturn(5L);
        given(stringRedisTemplate.getExpire("login:ratelimit:192.168.1.1")).willReturn(30L);

        // when
        aspect.checkRateLimit();

        // then
        then(valueOperations).should().increment("login:ratelimit:192.168.1.1");
        then(stringRedisTemplate).should().getExpire("login:ratelimit:192.168.1.1");
    }
}
