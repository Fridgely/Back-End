package soon.fridgely.global.security.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RefrigeratorAccessAspectUnitTest {

    @InjectMocks
    private RefrigeratorAccessAspect aspect;

    @Mock
    private RefrigeratorAccessValidator validator;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Test
    void SpEL을_통해_파라미터에서_key를_추출한다() {
        // given
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(1L, 1L);

        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getParameterNames()).willReturn(new String[]{"key", "other"});
        given(joinPoint.getArgs()).willReturn(new Object[]{key, "dummy"});

        ValidateRefrigeratorAccess annotation = mock(ValidateRefrigeratorAccess.class);
        given(annotation.key()).willReturn("#key");

        // when
        aspect.validateMembership(joinPoint, annotation);

        // then
        then(validator).should()
            .validateMembership(key);
    }

    @Test
    void SpEL을_통해_파라미터에서_dto의_toKey를_추출한다() {
        // given
        TestKey dto = new TestKey(1L, 1L);

        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getParameterNames()).willReturn(new String[]{"request"});
        given(joinPoint.getArgs()).willReturn(new Object[]{dto});

        ValidateRefrigeratorAccess annotation = mock(ValidateRefrigeratorAccess.class);
        given(annotation.key()).willReturn("#request.toKey()");

        // when
        aspect.validateMembership(joinPoint, annotation);

        // then
        then(validator).should()
            .validateMembership(dto.toKey());
    }

    @Test
    void SpEL이_null이면_예외가_발생한다() {
        // given
        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getParameterNames()).willReturn(new String[]{"key"});
        given(joinPoint.getArgs()).willReturn(new Object[]{null});

        ValidateRefrigeratorAccess annotation = mock(ValidateRefrigeratorAccess.class);
        given(annotation.key()).willReturn("#key");

        // expected
        assertThatThrownBy(() -> aspect.validateMembership(joinPoint, annotation))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REFRIGERATOR_ACCESS_KEY);
    }

    record TestKey(long memberId, long refrigeratorId) {
        public MemberRefrigeratorKey toKey() {
            return new MemberRefrigeratorKey(memberId, refrigeratorId);
        }
    }

}