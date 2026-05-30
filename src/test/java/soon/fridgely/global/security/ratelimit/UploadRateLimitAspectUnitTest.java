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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UploadRateLimitAspectUnitTest {

    @InjectMocks
    private UploadRateLimitAspect aspect;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aspect, "maxRequests", 10);
        ReflectionTestUtils.setField(aspect, "periodSeconds", 60L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 요청_횟수가_한도_이하이면_예외가_발생하지_않는다() {
        // given
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(5L);

        // when & then
        assertThatCode(() -> aspect.checkRateLimit()).doesNotThrowAnyException();
    }

    @Test
    void TTL이_없는_키는_expire를_설정한다() {
        // given
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(1L);
        given(stringRedisTemplate.getExpire("upload:ratelimit:1")).willReturn(-1L);

        // when
        aspect.checkRateLimit();

        // then
        then(stringRedisTemplate).should().expire(eq("upload:ratelimit:1"), any());
    }

    @Test
    void TTL_없는_기존_키는_방어적으로_expire를_설정한다() {
        // given: count가 1이 아니지만 이전 EXPIRE 실패로 TTL이 없는 경우
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(5L);
        given(stringRedisTemplate.getExpire("upload:ratelimit:1")).willReturn(-1L);

        // when
        aspect.checkRateLimit();

        // then
        then(stringRedisTemplate).should().expire(eq("upload:ratelimit:1"), any());
    }

    @Test
    void TTL이_설정된_키는_expire를_재설정하지_않는다() {
        // given
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(5L);
        given(stringRedisTemplate.getExpire("upload:ratelimit:1")).willReturn(30L);

        // when
        aspect.checkRateLimit();

        // then
        then(stringRedisTemplate).should(never()).expire(any(), any());
    }

    @Test
    void 요청_횟수가_한도를_초과하면_예외가_발생한다() {
        // given
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(11L);

        // expected
        assertThatThrownBy(() -> aspect.checkRateLimit())
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.UPLOAD_RATE_LIMIT_EXCEEDED);
    }

    @Test
    void 인증_정보가_없으면_예외가_발생한다() {
        // given
        SecurityContextHolder.clearContext();

        // expected
        assertThatThrownBy(() -> aspect.checkRateLimit())
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHENTICATION_FAILED);
    }

    @Test
    void 익명_사용자이면_예외가_발생한다() {
        // given
        Authentication anon = new AnonymousAuthenticationToken(
            "key", "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anon);

        // expected
        assertThatThrownBy(() -> aspect.checkRateLimit())
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHENTICATION_FAILED);
    }

    private void setAuthenticatedUser(Long userId) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            String.valueOf(userId), null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
