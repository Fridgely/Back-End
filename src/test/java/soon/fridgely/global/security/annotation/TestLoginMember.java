package soon.fridgely.global.security.annotation;

import org.springframework.security.test.context.support.WithSecurityContext;
import soon.fridgely.TestSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = TestSecurityContext.class)
public @interface TestLoginMember {

    long id() default 1L;

    String[] roles() default {"MEMBER"};

}