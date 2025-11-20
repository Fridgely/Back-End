package soon.fridgely.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateRefrigeratorAccess {

    /**
     * 검증에 사용할 MemberRefrigeratorKey를 찾기 위한 SpEL 표현식
     * ex) "#key", "#request.toKey()"
     */
    String key();

}