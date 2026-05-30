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
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(5L);

        assertThatCode(() -> aspect.checkRateLimit()).doesNotThrowAnyException();
    }

    @Test
    void 첫번째_요청이면_TTL을_설정한다() {
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(1L);

        aspect.checkRateLimit();

        then(stringRedisTemplate).should().expire(eq("upload:ratelimit:1"), any());
    }

    @Test
    void 첫번째_요청이_아니면_TTL을_재설정하지_않는다() {
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(5L);

        aspect.checkRateLimit();

        then(stringRedisTemplate).should(never()).expire(any(), any());
    }

    @Test
    void 요청_횟수가_한도를_초과하면_예외가_발생한다() {
        setAuthenticatedUser(1L);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("upload:ratelimit:1")).willReturn(11L);

        assertThatThrownBy(() -> aspect.checkRateLimit())
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.UPLOAD_RATE_LIMIT_EXCEEDED);
    }

    @Test
    void 인증_정보가_없으면_예외가_발생한다() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> aspect.checkRateLimit())
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.AUTHENTICATION_FAILED);
    }

    @Test
    void 익명_사용자이면_예외가_발생한다() {
        Authentication anon = new AnonymousAuthenticationToken(
            "key", "anonymous",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anon);

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
