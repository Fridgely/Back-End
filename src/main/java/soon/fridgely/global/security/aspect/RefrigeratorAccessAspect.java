package soon.fridgely.global.security.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

/**
 * 냉장고 접근 권한을 검증하는 AOP
 * Order=100으로 설정하여 Resilience4j RetryAspect보다 먼저 실행
 */
@RequiredArgsConstructor
@Aspect
@Component
@Order(100)
public class RefrigeratorAccessAspect {

    private final RefrigeratorAccessValidator validator;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Before("@annotation(validateRefrigeratorAccess)")
    public void validateMembership(JoinPoint joinPoint, ValidateRefrigeratorAccess validateRefrigeratorAccess) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        MemberRefrigeratorKey key = parser.parseExpression(validateRefrigeratorAccess.key())
            .getValue(context, MemberRefrigeratorKey.class);

        if (key == null) {
            throw new CoreException(ErrorType.INVALID_REFRIGERATOR_ACCESS_KEY);
        }

        validator.validateMembership(key);
    }

}